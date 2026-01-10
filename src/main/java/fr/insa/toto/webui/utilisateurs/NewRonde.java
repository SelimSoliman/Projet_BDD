package fr.insa.toto.webui.utilisateurs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.extensions.ListeTournoisView;
import fr.insa.toto.webui.session.SessionInfo;


import java.sql.Connection;
import java.sql.SQLException;

/**
 * Vue pour créer une nouvelle ronde sur le tournoi actif.
 */
@Route(value = "rondes/creer", layout = MainLayout.class)
@PageTitle("Créer une ronde")
public class NewRonde extends VerticalLayout {

    private Paragraph tournoiInfo;
    private Button creerRondeButton;
    private Button goToListeButton;

    public NewRonde() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add(new Paragraph("Seuls les administrateurs peuvent créer des rondes."));
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        add(new H2("Créer une nouvelle ronde"));

        // Vérifier qu'un tournoi est actif
        TournoiMulti tournoiActif = SessionInfo.getTournoiActif();

        if (tournoiActif == null) {
            // Aucun tournoi actif
            Paragraph warning = new Paragraph("⚠️ Aucun tournoi actif sélectionné.");
            warning.getStyle()
                .set("background-color", "#fff3e0")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #ff9800")
                .set("font-weight", "bold");
            add(warning);

            add(new Paragraph("Vous devez d'abord sélectionner un tournoi dans la liste " +
                             "avant de pouvoir créer une ronde."));
            
            goToListeButton = new Button("📋 Aller à la liste des tournois", e -> {
                getUI().ifPresent(ui -> ui.navigate(ListeTournoisView.class));
            });
            goToListeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(goToListeButton);
            return;
        }

        // Afficher les infos du tournoi actif
        tournoiInfo = new Paragraph();
        tournoiInfo.setText("🎯 Tournoi actif : " + tournoiActif.getNom() + 
                           " (ID: " + tournoiActif.getId() + 
                           ", " + tournoiActif.getNbTerrains() + " terrains, " +
                           tournoiActif.getNbJoueursParEquipe() + " joueurs/équipe)");
        tournoiInfo.getStyle()
            .set("background-color", "#e7f5ff")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("border-left", "4px solid #1976d2")
            .set("font-weight", "bold")
            .set("margin-bottom", "20px");
        add(tournoiInfo);

        add(new Paragraph("Cliquez sur le bouton ci-dessous pour créer une nouvelle ronde " +
                         "pour ce tournoi. La ronde sera créée avec tous les joueurs disponibles."));

        // Bouton pour créer la ronde
        creerRondeButton = new Button("✨ Créer la ronde", e -> creerRonde());
        creerRondeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        add(creerRondeButton);

        // Bouton pour changer de tournoi
        Button changerTournoiButton = new Button("🔄 Changer de tournoi", e -> {
            getUI().ifPresent(ui -> ui.navigate(ListeTournoisView.class));
        });
        add(changerTournoiButton);
    }

    private void creerRonde() {
        TournoiMulti tournoiActif = SessionInfo.getTournoiActif();
        
        if (tournoiActif == null) {
            Notification.show("❌ Aucun tournoi actif", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            // Créer la ronde pour le tournoi actif
            Ronde nouvelleRonde = Ronde.creeRondeAuto(con, tournoiActif.getId());

            Notification notification = Notification.show(
                "✅ Ronde créée avec succès pour le tournoi : " + tournoiActif.getNom() + 
                " (ID ronde: " + nouvelleRonde.getId() + ")", 
                5000, 
                Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification notification = Notification.show(
                "❌ Erreur lors de la création de la ronde : " + ex.getMessage(), 
                5000, 
                Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}