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

@Route(value = "rondes-matchs/gestion", layout = MainLayout.class)
@PageTitle("Gestion des rondes et matchs")
public class GestionRondesMatchsView extends VerticalLayout {

    private Grid<MatchInfo> gridMatchs;
    private Button creerRondeButton;
    private Button rafraichirButton;
    private ComboBox<TournoiMulti> tournoiCombo;
    private Paragraph stats;

    public GestionRondesMatchsView() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add(new Paragraph("Seuls les administrateurs peuvent gérer les rondes et matchs."));
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1400px");

        add(new H2("⚽ Gestion des rondes et matchs"));

        // Info rapide
        Paragraph info = new Paragraph();
        info.setText("💡 Sélectionnez un tournoi puis créez des rondes avec des matchs directement depuis cette interface.");
        info.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("border-left", "4px solid #2196f3");
        add(info);

        // Sélection du tournoi
        tournoiCombo = new ComboBox<>("Tournoi");
        tournoiCombo.setItemLabelGenerator(TournoiMulti::getNom);
        tournoiCombo.setWidthFull();
        tournoiCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                creerRondeButton.setEnabled(true);
                try {
                    chargerMatchs(e.getValue());
                } catch (SQLException ex) {
                    Notification.show("❌ Erreur : " + ex.getMessage(), 
                                    5000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                creerRondeButton.setEnabled(false);
            }
        });
        add(tournoiCombo);

        // Boutons d'action
        HorizontalLayout actionsLayout = new HorizontalLayout();
        
        creerRondeButton = new Button("➕ Créer une nouvelle ronde", e -> ouvrirDialogCreerRonde());
        creerRondeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        creerRondeButton.setEnabled(false);
        
        rafraichirButton = new Button("🔄 Rafraîchir", e -> {
            if (tournoiCombo.getValue() != null) {
                try {
                    chargerMatchs(tournoiCombo.getValue());
                } catch (SQLException ex) {
                    Notification.show("❌ Erreur : " + ex.getMessage(), 
                                    5000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        rafraichirButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        actionsLayout.add(creerRondeButton, rafraichirButton);
        add(actionsLayout);

        // Section MATCHS
        add(new H3("🎾 Matchs du tournoi"));
        
        gridMatchs = new Grid<>(MatchInfo.class, false);
        gridMatchs.addColumn(MatchInfo::getIdMatch).setHeader("ID Match").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getRonde).setHeader("Ronde").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getInfo).setHeader("Informations").setAutoWidth(true).setFlexGrow(1);
        
        gridMatchs.addComponentColumn(matchInfo -> {
            Button modifierButton = new Button("✏️ Scores");
            modifierButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            modifierButton.addClickListener(e -> ouvrirDialogModifierScores(matchInfo));
            return modifierButton;
        }).setHeader("Actions");
        
        gridMatchs.setWidthFull();
        add(gridMatchs);

        // Statistiques
        stats = new Paragraph();
        stats.getStyle()
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("margin-top", "20px");
        add(stats);

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

    private void chargerMatchs(TournoiMulti tournoi) throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Match> matchs = new ArrayList<>();
            
            String sql = "SELECT m.id, m.ronde_id, m.score_e1, m.score_e2, m.statut, r.numero as ronde_numero " +
                        "FROM matchs m " +
                        "INNER JOIN ronde r ON m.ronde_id = r.id " +
                        "WHERE r.id_tournoi = ? " +
                        "ORDER BY r.numero, m.id";
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, tournoi.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int matchId = rs.getInt("id");
                        int rondeNumero = rs.getInt("ronde_numero");
                        int scoreE1 = rs.getInt("score_e1");
                        int scoreE2 = rs.getInt("score_e2");
                        String statut = rs.getString("statut");
                        
                        Match m = new Match(matchId, null, null, scoreE1, scoreE2, 
                                          Match.Statut.valueOf(statut));
                        matchs.add(m);
                    }
                }
            }
            
            List<MatchInfo> matchsInfo = new ArrayList<>();
            
            for (Match m : matchs) {
                try {
                    StringBuilder info = new StringBuilder();
                    info.append("Scores: ").append(m.getScoreEquipe1());
                    info.append(" - ").append(m.getScoreEquipe2());
                    info.append(" | ").append(m.estClos() ? "✅ Terminé" : "⏳ En cours");
                    
                    // Récupérer le numéro de ronde
                    int rondeNum = getRondeNumero(con, m.getId());
                    
                    matchsInfo.add(new MatchInfo(m.getId(), rondeNum, info.toString()));
                    
                } catch (Exception ex) {
                    System.err.println("Erreur match " + m.getId() + ": " + ex.getMessage());
                }
            }
            
            gridMatchs.setItems(matchsInfo);
            
            if (matchsInfo.isEmpty()) {
                stats.setText("⚠️ Aucun match trouvé. Créez une ronde pour générer des matchs.");
                stats.getStyle().set("background-color", "#fff3cd");
            } else {
                long termines = matchsInfo.stream()
                    .filter(m -> m.getInfo().contains("Terminé"))
                    .count();
                stats.setText("📊 Total : " + matchsInfo.size() + " match(s) | " +
                             "✅ Terminés : " + termines + " | " +
                             "⏳ En cours : " + (matchsInfo.size() - termines));
                stats.getStyle().set("background-color", "#e8f5e9");
            }
            
        } catch (SQLException ex) {
            throw ex;
        }
    }

    private int getRondeNumero(Connection con, int matchId) throws SQLException {
        String sql = "SELECT r.numero FROM ronde r " +
                    "INNER JOIN matchs m ON m.ronde_id = r.id " +
                    "WHERE m.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("numero");
                }
            }
        }
        return 0;
    }

    private void ouvrirDialogCreerRonde() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("➕ Créer une nouvelle ronde");
        dialog.setWidth("600px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        TournoiMulti tournoi = tournoiCombo.getValue();
        
        Paragraph info = new Paragraph();
        info.setText("🎾 Tournoi : " + tournoi.getNom() + "\n" +
                    "🏟️ Terrains : " + tournoi.getNbTerrains() + "\n" +
                    "👥 Joueurs par équipe : " + tournoi.getNbJoueursParEquipe());
        info.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "10px")
            .set("border-radius", "5px")
            .set("white-space", "pre-line");
        layout.add(info);

        FormLayout form = new FormLayout();
        
        IntegerField nbMatchsField = new IntegerField("Nombre de matchs à créer");
        nbMatchsField.setValue(tournoi.getNbTerrains());
        nbMatchsField.setMin(1);
        nbMatchsField.setMax(20);
        nbMatchsField.setStepButtonsVisible(true);
        nbMatchsField.setHelperText("Généralement = nombre de terrains");
        
        form.add(nbMatchsField);
        layout.add(form);

        Paragraph warning = new Paragraph();
        warning.setText("⚠️ Les équipes seront générées aléatoirement parmi les joueurs inscrits au tournoi.");
        warning.getStyle()
            .set("background-color", "#fff3cd")
            .set("padding", "10px")
            .set("border-radius", "5px")
            .set("font-size", "14px");
        layout.add(warning);

        HorizontalLayout buttons = new HorizontalLayout();
        
        Button creerButton = new Button("✅ Créer la ronde", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                int nbMatchs = nbMatchsField.getValue();
                
                // Créer la ronde avec des matchs
                boolean success = creerNouvelleRonde(con, tournoi, nbMatchs);
                
                if (success) {
                    Notification.show("✅ Ronde créée avec " + nbMatchs + " match(s) !", 
                                    3000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                    chargerMatchs(tournoi);
                    dialog.close();
                } else {
                    Notification.show("❌ Erreur lors de la création de la ronde", 
                                    5000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                
            } catch (SQLException ex) {
                Notification.show("❌ Erreur : " + ex.getMessage(), 
                                5000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });
        creerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        
        Button annulerButton = new Button("Annuler", e -> dialog.close());
        
        buttons.add(creerButton, annulerButton);
        layout.add(buttons);
        
        dialog.add(layout);
        dialog.open();
    }

    private boolean creerNouvelleRonde(Connection con, TournoiMulti tournoi, int nbMatchs) throws SQLException {
        try {
            // 1. Compter les rondes existantes
            int numeroRonde = 1;
            String sqlCount = "SELECT MAX(numero) as max_numero FROM ronde WHERE id_tournoi = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlCount)) {
                ps.setInt(1, tournoi.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("max_numero") > 0) {
                        numeroRonde = rs.getInt("max_numero") + 1;
                    }
                }
            }
            
            // 2. Créer la ronde
            int rondeId;
            String sqlRonde = "INSERT INTO ronde (id_tournoi, numero, debut, close) VALUES (?, ?, NOW(), 0)";
            try (PreparedStatement ps = con.prepareStatement(sqlRonde, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, tournoi.getId());
                ps.setInt(2, numeroRonde);
                ps.executeUpdate();
                
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        rondeId = rs.getInt(1);
                    } else {
                        return false;
                    }
                }
            }
            
            // 3. Récupérer les terrains
            List<Terrain> terrains = Terrain.tousLesTerrains(con);
            if (terrains.isEmpty()) {
                throw new SQLException("Aucun terrain disponible. Créez d'abord des terrains.");
            }
            
            // 4. Récupérer les joueurs inscrits
            List<Joueur> joueurs = tournoi.getJoueursInscrits(con);
            if (joueurs.size() < nbMatchs * tournoi.getNbJoueursParEquipe() * 2) {
                throw new SQLException("Pas assez de joueurs inscrits. Il faut au moins " + 
                                      (nbMatchs * tournoi.getNbJoueursParEquipe() * 2) + " joueurs.");
            }
            
            // 5. Créer les matchs
            int joueursParEquipe = tournoi.getNbJoueursParEquipe();
            int joueurIndex = 0;
            
            for (int i = 0; i < nbMatchs && joueurIndex + (joueursParEquipe * 2) <= joueurs.size(); i++) {
                // Terrain assigné (rotation)
                Terrain terrain = terrains.get(i % terrains.size());
                
                // Créer le match
                String sqlMatch = "INSERT INTO matchs (ronde_id, terrain_id, score_e1, score_e2, statut) VALUES (?, ?, 0, 0, 'EN_COURS')";
                int matchId;
                try (PreparedStatement ps = con.prepareStatement(sqlMatch, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, rondeId);
                    ps.setInt(2, terrain.getId());
                    ps.executeUpdate();
                    
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            matchId = rs.getInt(1);
                        } else {
                            continue;
                        }
                    }
                }
                
                // Créer équipe 1
                int equipe1Id = creerEquipe(con, matchId, 1);
                for (int j = 0; j < joueursParEquipe && joueurIndex < joueurs.size(); j++) {
                    ajouterJoueurEquipe(con, equipe1Id, joueurs.get(joueurIndex++).getId());
                }
                
                // Créer équipe 2
                int equipe2Id = creerEquipe(con, matchId, 2);
                for (int j = 0; j < joueursParEquipe && joueurIndex < joueurs.size(); j++) {
                    ajouterJoueurEquipe(con, equipe2Id, joueurs.get(joueurIndex++).getId());
                }
            }
            
            return true;
            
        } catch (SQLException ex) {
            throw ex;
        }
    }

    private int creerEquipe(Connection con, int matchId, int numero) throws SQLException {
        String sql = "INSERT INTO equipe (id_match, numero, score) VALUES (?, ?, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, matchId);
            ps.setInt(2, numero);
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Impossible de créer l'équipe");
    }

    private void ajouterJoueurEquipe(Connection con, int equipeId, int joueurId) throws SQLException {
        String sql = "INSERT INTO match_joueur (id_equipe, id_joueur) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, joueurId);
            ps.executeUpdate();
        }
    }

    private void ouvrirDialogModifierScores(MatchInfo matchInfo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("✏️ Modifier les scores");

        FormLayout form = new FormLayout();
        
        IntegerField score1Field = new IntegerField("Score Équipe 1");
        score1Field.setValue(extraireScore1(matchInfo.getInfo()));
        score1Field.setMin(0);
        score1Field.setStepButtonsVisible(true);
        
        IntegerField score2Field = new IntegerField("Score Équipe 2");
        score2Field.setValue(extraireScore2(matchInfo.getInfo()));
        score2Field.setMin(0);
        score2Field.setStepButtonsVisible(true);
        
        form.add(score1Field, score2Field);

        HorizontalLayout buttons = new HorizontalLayout();
        
        Button sauvegarderButton = new Button("💾 Sauvegarder", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                String sql = "UPDATE matchs SET score_e1 = ?, score_e2 = ? WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, score1Field.getValue());
                    ps.setInt(2, score2Field.getValue());
                    ps.setInt(3, matchInfo.getIdMatch());
                    ps.executeUpdate();
                }
                
                // Mettre à jour les équipes aussi
                String sqlEquipes = "UPDATE equipe SET score = ? WHERE id_match = ? AND numero = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlEquipes)) {
                    ps.setInt(1, score1Field.getValue());
                    ps.setInt(2, matchInfo.getIdMatch());
                    ps.setInt(3, 1);
                    ps.executeUpdate();
                    
                    ps.setInt(1, score2Field.getValue());
                    ps.setInt(3, 2);
                    ps.executeUpdate();
                }
                
                Notification.show("✅ Scores mis à jour !", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                chargerMatchs(tournoiCombo.getValue());
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
        
        VerticalLayout layout = new VerticalLayout(form, buttons);
        dialog.add(layout);
        dialog.open();
    }

    private int extraireScore1(String info) {
        try {
            String scores = info.substring(info.indexOf("Scores: ") + 8);
            String score1 = scores.substring(0, scores.indexOf(" -")).trim();
            return Integer.parseInt(score1);
        } catch (Exception e) {
            return 0;
        }
    }

    private int extraireScore2(String info) {
        try {
            String scores = info.substring(info.indexOf("- ") + 2);
            String score2 = scores.substring(0, scores.indexOf(" |")).trim();
            return Integer.parseInt(score2);
        } catch (Exception e) {
            return 0;
        }
    }

    public static class MatchInfo {
        private int idMatch;
        private int ronde;
        private String info;

        public MatchInfo(int idMatch, int ronde, String info) {
            this.idMatch = idMatch;
            this.ronde = ronde;
            this.info = info;
        }

        public int getIdMatch() { return idMatch; }
        public int getRonde() { return ronde; }
        public String getInfo() { return info; }
    }
}