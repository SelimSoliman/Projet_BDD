package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "rondes-matchs/resultats", layout = MainLayout.class)
@PageTitle("Résultats des matchs")
public class GestionResultatsMatchsView extends VerticalLayout {

    private ComboBox<TournoiMulti> tournoiCombo;
    private ComboBox<RondeInfo> rondeCombo;
    private Grid<MatchDetailInfo> gridMatchs;
    private Button rafraichirButton;
    private Button clorerRondeButton;
    private Paragraph statsRonde;

    public GestionResultatsMatchsView() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add(new Paragraph("Seuls les administrateurs peuvent gérer les résultats des matchs."));
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1400px");

        add(new H2("🎯 Gestion des résultats des matchs"));

        // Info
        Paragraph info = new Paragraph();
        info.setText("💡 Sélectionnez un tournoi et une ronde pour saisir les résultats des matchs.");
        info.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("border-left", "4px solid #2196f3");
        add(info);

        // Sélection tournoi
        tournoiCombo = new ComboBox<>("Tournoi");
        tournoiCombo.setItemLabelGenerator(TournoiMulti::getNom);
        tournoiCombo.setWidthFull();
        tournoiCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                chargerRondes(e.getValue());
            } else {
                rondeCombo.clear();
                rondeCombo.setItems();
            }
        });
        add(tournoiCombo);

        // Sélection ronde
        rondeCombo = new ComboBox<>("Ronde");
        rondeCombo.setItemLabelGenerator(r -> "Ronde " + r.getNumero() + " - " + 
                                              (r.isClose() ? "✅ Clôturée" : "⏳ En cours"));
        rondeCombo.setWidthFull();
        rondeCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                try {
                    chargerMatchs(e.getValue());
                } catch (SQLException ex) {
                    Notification.show("❌ Erreur : " + ex.getMessage(), 
                                    5000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        add(rondeCombo);

        // Boutons d'action
        HorizontalLayout actionsLayout = new HorizontalLayout();
        
        rafraichirButton = new Button("🔄 Rafraîchir", e -> {
            if (rondeCombo.getValue() != null) {
                try {
                    chargerMatchs(rondeCombo.getValue());
                } catch (SQLException ex) {
                    Notification.show("❌ Erreur : " + ex.getMessage(), 
                                    5000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        rafraichirButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        clorerRondeButton = new Button("🔒 Clôturer la ronde", e -> cloturerRonde());
        clorerRondeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        clorerRondeButton.setEnabled(false);
        
        actionsLayout.add(rafraichirButton, clorerRondeButton);
        add(actionsLayout);

        // Stats de la ronde
        statsRonde = new Paragraph();
        statsRonde.getStyle()
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("margin-top", "10px");
        add(statsRonde);

        // Grille des matchs
        add(new H3("📋 Matchs de la ronde"));
        
        gridMatchs = new Grid<>(MatchDetailInfo.class, false);
        gridMatchs.addColumn(MatchDetailInfo::getIdMatch).setHeader("ID").setAutoWidth(true);
        gridMatchs.addColumn(MatchDetailInfo::getTerrain).setHeader("Terrain").setAutoWidth(true);
        gridMatchs.addColumn(MatchDetailInfo::getEquipe1).setHeader("Équipe 1").setFlexGrow(1);
        gridMatchs.addColumn(MatchDetailInfo::getScoreE1).setHeader("Score 1").setAutoWidth(true);
        gridMatchs.addColumn(MatchDetailInfo::getScoreE2).setHeader("Score 2").setAutoWidth(true);
        gridMatchs.addColumn(MatchDetailInfo::getEquipe2).setHeader("Équipe 2").setFlexGrow(1);
        gridMatchs.addColumn(m -> m.isTermine() ? "✅ Terminé" : "⏳ En cours")
                 .setHeader("Statut").setAutoWidth(true);
        
        gridMatchs.addComponentColumn(match -> {
            Button modifierButton = new Button("✏️ Résultats");
            modifierButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            modifierButton.addClickListener(e -> ouvrirDialogResultats(match));
            return modifierButton;
        }).setHeader("Actions");
        
        gridMatchs.setWidthFull();
        add(gridMatchs);

        // Chargement initial
        try {
            chargerTournois();
        } catch (SQLException ex) {
            Notification.show("❌ Erreur de chargement : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerTournois() throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti> tournois = TournoiMulti.tousLesTournois(con);
            tournoiCombo.setItems(tournois);
            
            if (!tournois.isEmpty()) {
                tournoiCombo.setValue(tournois.get(0));
            }
        }
    }

    private void chargerRondes(TournoiMulti tournoi) {
        try (Connection con = ConnectionPool.getConnection()) {
            List<RondeInfo> rondes = new ArrayList<>();
            
            String sql = "SELECT id, numero, close FROM ronde WHERE id_tournoi = ? ORDER BY numero DESC";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, tournoi.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rondes.add(new RondeInfo(
                            rs.getInt("id"),
                            rs.getInt("numero"),
                            rs.getBoolean("close")
                        ));
                    }
                }
            }
            
            rondeCombo.setItems(rondes);
            if (!rondes.isEmpty()) {
                rondeCombo.setValue(rondes.get(0));
            }
            
        } catch (SQLException ex) {
            Notification.show("❌ Erreur : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerMatchs(RondeInfo ronde) throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            List<MatchDetailInfo> matchs = new ArrayList<>();
            
            String sql = """
                SELECT m.id, m.score_e1, m.score_e2, m.statut, t.nom as terrain_nom
                FROM matchs m
                LEFT JOIN terrain t ON m.terrain_id = t.id
                WHERE m.ronde_id = ?
                ORDER BY m.id
                """;
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, ronde.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int matchId = rs.getInt("id");
                        
                        // Récupérer les joueurs des équipes
                        String equipe1 = getJoueursEquipe(con, matchId, 1);
                        String equipe2 = getJoueursEquipe(con, matchId, 2);
                        
                        matchs.add(new MatchDetailInfo(
                            matchId,
                            rs.getString("terrain_nom"),
                            equipe1,
                            equipe2,
                            rs.getInt("score_e1"),
                            rs.getInt("score_e2"),
                            "CLOS".equals(rs.getString("statut"))
                        ));
                    }
                }
            }
            
            gridMatchs.setItems(matchs);
            updateStatsRonde(ronde, matchs);
            clorerRondeButton.setEnabled(!ronde.isClose() && tousMatchsTermines(matchs));
            
        }
    }

    private String getJoueursEquipe(Connection con, int matchId, int numeroEquipe) throws SQLException {
        StringBuilder joueurs = new StringBuilder();
        
        String sql = """
            SELECT j.surnom
            FROM match_joueur mj
            JOIN joueur j ON mj.id_joueur = j.id
            WHERE mj.id_match = ? AND mj.numero_equipe = ?
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, numeroEquipe);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (joueurs.length() > 0) joueurs.append(", ");
                    joueurs.append(rs.getString("surnom"));
                }
            }
        }
        
        return joueurs.length() > 0 ? joueurs.toString() : "(vide)";
    }

    private boolean tousMatchsTermines(List<MatchDetailInfo> matchs) {
        return matchs.stream().allMatch(MatchDetailInfo::isTermine);
    }

    private void updateStatsRonde(RondeInfo ronde, List<MatchDetailInfo> matchs) {
        long nbTermines = matchs.stream().filter(MatchDetailInfo::isTermine).count();
        long nbEnCours = matchs.size() - nbTermines;
        
        String statut = ronde.isClose() ? 
            "🔒 Ronde clôturée" : 
            "⏳ Ronde en cours - " + nbTermines + " match(s) terminé(s), " + nbEnCours + " en cours";
        
        statsRonde.setText(statut);
        
        if (ronde.isClose()) {
            statsRonde.getStyle()
                .set("background-color", "#e8f5e9")
                .set("border-left", "4px solid #4caf50");
        } else {
            statsRonde.getStyle()
                .set("background-color", "#fff3e0")
                .set("border-left", "4px solid #ff9800");
        }
    }

    private void ouvrirDialogResultats(MatchDetailInfo match) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("✏️ Saisir les résultats");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        // Info du match
        Div matchInfo = new Div();
        matchInfo.setText("Match #" + match.getIdMatch() + " - " + match.getTerrain());
        matchInfo.getStyle()
            .set("font-weight", "bold")
            .set("margin-bottom", "10px");
        content.add(matchInfo);

        FormLayout form = new FormLayout();
        
        // Équipe 1
        Div equipe1Label = new Div();
        equipe1Label.setText("Équipe 1: " + match.getEquipe1());
        equipe1Label.getStyle().set("font-weight", "bold");
        
        IntegerField score1Field = new IntegerField("Score Équipe 1");
        score1Field.setValue(match.getScoreE1());
        score1Field.setMin(0);
        score1Field.setStepButtonsVisible(true);
        score1Field.setWidthFull();
        
        // Équipe 2
        Div equipe2Label = new Div();
        equipe2Label.setText("Équipe 2: " + match.getEquipe2());
        equipe2Label.getStyle().set("font-weight", "bold");
        
        IntegerField score2Field = new IntegerField("Score Équipe 2");
        score2Field.setValue(match.getScoreE2());
        score2Field.setMin(0);
        score2Field.setStepButtonsVisible(true);
        score2Field.setWidthFull();
        
        form.add(equipe1Label, score1Field, equipe2Label, score2Field);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        content.add(form);

        // Boutons
        HorizontalLayout buttons = new HorizontalLayout();
        
        Button sauvegarderButton = new Button("💾 Enregistrer", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                // Mettre à jour les scores du match
                String sqlMatch = "UPDATE matchs SET score_e1 = ?, score_e2 = ?, statut = 'CLOS' WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlMatch)) {
                    ps.setInt(1, score1Field.getValue());
                    ps.setInt(2, score2Field.getValue());
                    ps.setInt(3, match.getIdMatch());
                    ps.executeUpdate();
                }
                
                // Mettre à jour les scores des équipes
                String sqlEquipe = "UPDATE equipe SET score = ? WHERE id_match = ? AND numero = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlEquipe)) {
                    ps.setInt(1, score1Field.getValue());
                    ps.setInt(2, match.getIdMatch());
                    ps.setInt(3, 1);
                    ps.executeUpdate();
                    
                    ps.setInt(1, score2Field.getValue());
                    ps.setInt(3, 2);
                    ps.executeUpdate();
                }
                
                Notification.show("✅ Résultats enregistrés !", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                chargerMatchs(rondeCombo.getValue());
                dialog.close();
                
            } catch (SQLException ex) {
                Notification.show("❌ Erreur : " + ex.getMessage(), 
                                5000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        sauvegarderButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button annulerButton = new Button("Annuler", e -> dialog.close());
        
        buttons.add(sauvegarderButton, annulerButton);
        content.add(buttons);
        
        dialog.add(content);
        dialog.open();
    }

    private void cloturerRonde() {
        RondeInfo ronde = rondeCombo.getValue();
        if (ronde == null) return;
        
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("⚠️ Confirmation");
        
        VerticalLayout content = new VerticalLayout();
        content.add(new Paragraph("Êtes-vous sûr de vouloir clôturer cette ronde ?"));
        content.add(new Paragraph("Cette action est irréversible."));
        
        HorizontalLayout buttons = new HorizontalLayout();
        Button confirmerButton = new Button("Oui, clôturer", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                String sql = "UPDATE ronde SET close = 1 WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, ronde.getId());
                    ps.executeUpdate();
                }
                
                Notification.show("✅ Ronde clôturée avec succès !", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                confirmDialog.close();
                chargerRondes(tournoiCombo.getValue());
                
            } catch (SQLException ex) {
                Notification.show("❌ Erreur : " + ex.getMessage(), 
                                5000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        
        Button annulerButton = new Button("Annuler", e -> confirmDialog.close());
        
        buttons.add(confirmerButton, annulerButton);
        content.add(buttons);
        
        confirmDialog.add(content);
        confirmDialog.open();
    }

    // Classes internes
    public static class RondeInfo {
        private int id;
        private int numero;
        private boolean close;

        public RondeInfo(int id, int numero, boolean close) {
            this.id = id;
            this.numero = numero;
            this.close = close;
        }

        public int getId() { return id; }
        public int getNumero() { return numero; }
        public boolean isClose() { return close; }
    }

    public static class MatchDetailInfo {
        private int idMatch;
        private String terrain;
        private String equipe1;
        private String equipe2;
        private int scoreE1;
        private int scoreE2;
        private boolean termine;

        public MatchDetailInfo(int idMatch, String terrain, String equipe1, String equipe2,
                              int scoreE1, int scoreE2, boolean termine) {
            this.idMatch = idMatch;
            this.terrain = terrain != null ? terrain : "Sans terrain";
            this.equipe1 = equipe1;
            this.equipe2 = equipe2;
            this.scoreE1 = scoreE1;
            this.scoreE2 = scoreE2;
            this.termine = termine;
        }

        public int getIdMatch() { return idMatch; }
        public String getTerrain() { return terrain; }
        public String getEquipe1() { return equipe1; }
        public String getEquipe2() { return equipe2; }
        public int getScoreE1() { return scoreE1; }
        public int getScoreE2() { return scoreE2; }
        public boolean isTermine() { return termine; }
    }
}