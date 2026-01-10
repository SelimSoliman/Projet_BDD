package fr.insa.toto.webui.matchs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.extensions.ListeTournoisView;
import fr.insa.toto.webui.session.SessionInfo;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * VERSION POUR MATCH EN CLASSE CLASSIQUE (avec getters/setters)
 */
@Route(value = "matchs/resultats", layout = MainLayout.class)
@PageTitle("Résultat des matchs")
public class ResultatMatch extends VerticalLayout {

    private Paragraph tournoiInfo;
    private Grid<Match> matchGrid;
    private IntegerField scoreEquipe1Field;
    private IntegerField scoreEquipe2Field;
    private Button sauvegarderButton;
    private Button rafraichirButton;

    public ResultatMatch() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Saisie des résultats de matchs"));

        TournoiMulti tournoiActif = SessionInfo.getTournoiActif();

        if (tournoiActif == null) {
            Paragraph warning = new Paragraph("⚠️ Aucun tournoi actif sélectionné.");
            warning.getStyle()
                .set("background-color", "#fff3e0")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #ff9800")
                .set("font-weight", "bold");
            add(warning);

            add(new Paragraph("Vous devez d'abord sélectionner un tournoi dans la liste."));
            
            Button goToListeButton = new Button("📋 Aller à la liste des tournois", e -> {
                getUI().ifPresent(ui -> ui.navigate(ListeTournoisView.class));
            });
            goToListeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(goToListeButton);
            return;
        }

        tournoiInfo = new Paragraph();
        tournoiInfo.setText("🎯 Tournoi actif : " + tournoiActif.getNom() + 
                           " (ID: " + tournoiActif.getId() + ")");
        tournoiInfo.getStyle()
            .set("background-color", "#e7f5ff")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("border-left", "4px solid #1976d2")
            .set("font-weight", "bold")
            .set("margin-bottom", "20px");
        add(tournoiInfo);

        // Grid avec syntaxe GETTERS
        matchGrid = new Grid<>(Match.class, false);
        matchGrid.addColumn(Match::getId).setHeader("ID Match").setAutoWidth(true);
        matchGrid.addColumn(Match::getIdRonde).setHeader("ID Ronde").setAutoWidth(true);
        matchGrid.addColumn(Match::getIdEquipe1).setHeader("Équipe 1").setAutoWidth(true);
        matchGrid.addColumn(Match::getIdEquipe2).setHeader("Équipe 2").setAutoWidth(true);
        
        matchGrid.addColumn(match -> {
            if (match.getScoreEquipe1() == -1) {
                return "Non joué";
            }
            return match.getScoreEquipe1() + " - " + match.getScoreEquipe2();
        }).setHeader("Score").setAutoWidth(true);

        matchGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        matchGrid.asSingleSelect().addValueChangeListener(event -> {
            Match selected = event.getValue();
            if (selected != null) {
                if (selected.getScoreEquipe1() != -1) {
                    scoreEquipe1Field.setValue(selected.getScoreEquipe1());
                    scoreEquipe2Field.setValue(selected.getScoreEquipe2());
                } else {
                    scoreEquipe1Field.clear();
                    scoreEquipe2Field.clear();
                }
                sauvegarderButton.setEnabled(true);
            } else {
                sauvegarderButton.setEnabled(false);
            }
        });

        add(matchGrid);

        HorizontalLayout scoreLayout = new HorizontalLayout();
        scoreLayout.setWidthFull();
        scoreLayout.setAlignItems(Alignment.END);

        scoreEquipe1Field = new IntegerField("Score équipe 1");
        scoreEquipe1Field.setMin(0);
        scoreEquipe1Field.setPlaceholder("0");

        scoreEquipe2Field = new IntegerField("Score équipe 2");
        scoreEquipe2Field.setMin(0);
        scoreEquipe2Field.setPlaceholder("0");

        sauvegarderButton = new Button("💾 Sauvegarder", e -> sauvegarderScore());
        sauvegarderButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sauvegarderButton.setEnabled(false);

        rafraichirButton = new Button("🔄 Rafraîchir", e -> chargerMatchs(tournoiActif));

        scoreLayout.add(scoreEquipe1Field, scoreEquipe2Field, sauvegarderButton, rafraichirButton);
        add(scoreLayout);

        chargerMatchs(tournoiActif);
    }

    private void chargerMatchs(TournoiMulti tournoi) {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Ronde> rondes = Ronde.toutesLesRondesDuTournoi(con, tournoi.getId());
            
            if (rondes.isEmpty()) {
                Notification.show("⚠️ Aucune ronde trouvée pour ce tournoi.", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_WARNING);
                matchGrid.setItems(new ArrayList<>());
                return;
            }

            List<Match> tousLesMatchs = new ArrayList<>();
            for (Ronde ronde : rondes) {
                List<Match> matchsDeLaRonde = Match.tousLesMatchsDeLaRonde(con, ronde.getId());
                tousLesMatchs.addAll(matchsDeLaRonde);
            }

            if (tousLesMatchs.isEmpty()) {
                Notification.show("ℹ️ Aucun match trouvé pour ce tournoi.", 
                                3000, Notification.Position.MIDDLE);
            }

            matchGrid.setItems(tousLesMatchs);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void sauvegarderScore() {
        Match selected = matchGrid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("⚠️ Veuillez sélectionner un match", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Integer score1 = scoreEquipe1Field.getValue();
        Integer score2 = scoreEquipe2Field.getValue();

        if (score1 == null || score2 == null) {
            Notification.show("⚠️ Veuillez saisir les deux scores", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            // Utiliser les setters (syntaxe CLASSE CLASSIQUE)
            selected.setScoreEquipe1(score1);
            selected.setScoreEquipe2(score2);
            selected.updateInDB(con);

            Notification.show("✅ Score sauvegardé : " + score1 + " - " + score2, 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            TournoiMulti tournoiActif = SessionInfo.getTournoiActif();
            if (tournoiActif != null) {
                chargerMatchs(tournoiActif);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}