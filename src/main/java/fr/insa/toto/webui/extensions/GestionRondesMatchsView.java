package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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

        add(new Paragraph("💡 Cette page permet de créer des rondes et de saisir les résultats des matchs."));

        // Sélection du tournoi
        tournoiCombo = new ComboBox<>("Tournoi");
        tournoiCombo.setItemLabelGenerator(TournoiMulti::getNom);
        tournoiCombo.setWidthFull();
        tournoiCombo.addValueChangeListener(e -> {
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
        add(tournoiCombo);

        // Bouton créer ronde
        creerRondeButton = new Button("➕ Créer une nouvelle ronde", e -> ouvrirDialogCreerRonde());
        creerRondeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        creerRondeButton.setEnabled(false);
        add(creerRondeButton);

        // Section MATCHS
        add(new H3("🎾 Matchs du tournoi"));
        
        gridMatchs = new Grid<>(MatchInfo.class, false);
        gridMatchs.addColumn(MatchInfo::getIdMatch).setHeader("ID Match").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getTerrain).setHeader("Terrain").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getEquipe1).setHeader("Équipe 1").setWidth("200px");
        gridMatchs.addColumn(MatchInfo::getScore1).setHeader("Score 1").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getEquipe2).setHeader("Équipe 2").setWidth("200px");
        gridMatchs.addColumn(MatchInfo::getScore2).setHeader("Score 2").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getStatut).setHeader("Statut").setAutoWidth(true);
        
        gridMatchs.addComponentColumn(matchInfo -> {
            Button saisirButton = new Button("✏️ Résultat");
            saisirButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            saisirButton.addClickListener(e -> ouvrirDialogSaisirResultat(matchInfo));
            return saisirButton;
        }).setHeader("Actions");
        
        gridMatchs.setWidthFull();
        add(gridMatchs);

        // Statistiques
        stats = new Paragraph();
        stats.getStyle()
            .set("background-color", "#e8f5e9")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("margin-top", "20px");
        add(stats);

        // Bouton rafraîchir
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
        add(rafraichirButton);

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
            
            // Sélectionner le premier tournoi
            if (!tournois.isEmpty()) {
                tournoiCombo.setValue(tournois.get(0));
            }
        }
    }

    private void chargerMatchs(TournoiMulti tournoi) throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            // Récupérer tous les matchs du tournoi
            List<Match> matchs = Match.tousLesMatchsDuTournoi(con, tournoi.getId());
            
            List<MatchInfo> matchsInfo = new ArrayList<>();
            for (Match m : matchs) {
                String terrain = m.getTerrain() != null ? m.getTerrain().getNom() : "Non assigné";
                
                // Récupérer les joueurs de l'équipe 1
                List<Joueur> joueursE1 = m.getEquipe1().getJoueurs();
                String equipe1 = joueursE1.isEmpty() ? 
                    "Équipe " + m.getEquipe1().getNumero() : 
                    joueursE1.stream()
                        .map(Joueur::getSurnom)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Équipe " + m.getEquipe1().getNumero());
                
                // Récupérer les joueurs de l'équipe 2
                List<Joueur> joueursE2 = m.getEquipe2().getJoueurs();
                String equipe2 = joueursE2.isEmpty() ? 
                    "Équipe " + m.getEquipe2().getNumero() : 
                    joueursE2.stream()
                        .map(Joueur::getSurnom)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Équipe " + m.getEquipe2().getNumero());
                
                matchsInfo.add(new MatchInfo(
                    m.getId(),
                    terrain,
                    equipe1,
                    m.getEquipe1().getScoreTotal(),
                    equipe2,
                    m.getEquipe2().getScoreTotal(),
                    m.estTermine() ? "✅ Terminé" : "⏳ En cours"
                ));
            }
            
            gridMatchs.setItems(matchsInfo);
            
            long termines = matchsInfo.stream().filter(m -> m.getStatut().contains("Terminé")).count();
            long enCours = matchsInfo.size() - termines;
            
            stats.setText("📊 Total : " + matchsInfo.size() + " match(s) | " +
                         "✅ Terminés : " + termines + " | " +
                         "⏳ En cours : " + enCours);

            creerRondeButton.setEnabled(true);
            
        } catch (SQLException ex) {
            throw ex;
        }
    }

    private void ouvrirDialogCreerRonde() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("➕ Créer une nouvelle ronde");

        VerticalLayout layout = new VerticalLayout();
        layout.add(new Paragraph("⚠️ Fonctionnalité de création de ronde à implémenter."));
        layout.add(new Paragraph("💡 Cette fonctionnalité nécessite la logique métier de génération de rondes depuis le modèle TournoiMulti."));
        layout.add(new Paragraph("📋 Pour l'instant, vous pouvez créer des rondes via la console (MainConsole.java)."));

        HorizontalLayout buttons = new HorizontalLayout();
        
        Button fermerButton = new Button("Fermer", e -> dialog.close());
        fermerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        buttons.add(fermerButton);
        layout.add(buttons);
        
        dialog.add(layout);
        dialog.open();
    }

    private void ouvrirDialogSaisirResultat(MatchInfo matchInfo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("✏️ Saisir le résultat du match");

        FormLayout form = new FormLayout();
        
        Paragraph infos = new Paragraph(
            "Match : " + matchInfo.getEquipe1() + " vs " + matchInfo.getEquipe2()
        );
        
        IntegerField score1Field = new IntegerField("Score " + matchInfo.getEquipe1());
        score1Field.setValue(matchInfo.getScore1());
        score1Field.setMin(0);
        score1Field.setStepButtonsVisible(true);
        
        IntegerField score2Field = new IntegerField("Score " + matchInfo.getEquipe2());
        score2Field.setValue(matchInfo.getScore2());
        score2Field.setMin(0);
        score2Field.setStepButtonsVisible(true);
        
        form.add(infos, score1Field, score2Field);

        HorizontalLayout buttons = new HorizontalLayout();
        
        Button sauvegarderButton = new Button("💾 Sauvegarder", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                Match match = Match.trouver(con, matchInfo.getIdMatch());
                
                if (match != null) {
                    match.getEquipe1().setScoreTotal(score1Field.getValue());
                    match.getEquipe2().setScoreTotal(score2Field.getValue());
                    
                    // Sauvegarder en base
                    match.getEquipe1().updateInDB(con);
                    match.getEquipe2().updateInDB(con);
                    
                    Notification.show("✅ Résultat enregistré !", 
                                    3000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                    chargerMatchs(tournoiCombo.getValue());
                    dialog.close();
                } else {
                    Notification.show("❌ Match introuvable", 
                                    5000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
                
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

    // Classe interne pour affichage des matchs
    public static class MatchInfo {
        private int idMatch;
        private String terrain;
        private String equipe1;
        private int score1;
        private String equipe2;
        private int score2;
        private String statut;

        public MatchInfo(int idMatch, String terrain, String equipe1, int score1, 
                        String equipe2, int score2, String statut) {
            this.idMatch = idMatch;
            this.terrain = terrain;
            this.equipe1 = equipe1;
            this.score1 = score1;
            this.equipe2 = equipe2;
            this.score2 = score2;
            this.statut = statut;
        }

        public int getIdMatch() { return idMatch; }
        public String getTerrain() { return terrain; }
        public String getEquipe1() { return equipe1; }
        public int getScore1() { return score1; }
        public String getEquipe2() { return equipe2; }
        public int getScore2() { return score2; }
        public String getStatut() { return statut; }
    }
}