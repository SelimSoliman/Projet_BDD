package fr.insa.toto.webui.tournois;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "tournois/liste", layout = MainLayout.class)
@PageTitle("Liste des tournois")
public class ListeTournoisView extends VerticalLayout {

    private Grid<TournoiMulti> grid;
    private Paragraph tournoiActifInfo;
    private Button selectionnerButton;
    private Button supprimerButton;
    private Button rafraichirButton;

    public ListeTournoisView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Liste des tournois"));

        // Afficher le tournoi actuellement actif
        tournoiActifInfo = new Paragraph();
        updateTournoiActifInfo();
        add(tournoiActifInfo);

        // Grid pour afficher tous les tournois
        grid = new Grid<>(TournoiMulti.class, false);
        grid.addColumn(TournoiMulti::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(TournoiMulti::getNom).setHeader("Nom").setAutoWidth(true);
        grid.addColumn(TournoiMulti::getNbTerrains).setHeader("Terrains").setAutoWidth(true);
        grid.addColumn(TournoiMulti::getNbJoueursParEquipe).setHeader("Joueurs/équipe").setAutoWidth(true);
        
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.asSingleSelect().addValueChangeListener(event -> {
            TournoiMulti selected = event.getValue();
            selectionnerButton.setEnabled(selected != null);
            supprimerButton.setEnabled(selected != null && SessionInfo.adminConnected());
        });

        add(grid);

        // Boutons d'action
        HorizontalLayout actions = new HorizontalLayout();

        selectionnerButton = new Button("Sélectionner comme tournoi actif", e -> selectionnerTournoi());
        selectionnerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        selectionnerButton.setEnabled(false);

        supprimerButton = new Button("Supprimer", e -> supprimerTournoi());
        supprimerButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        supprimerButton.setEnabled(false);
        supprimerButton.setVisible(SessionInfo.adminConnected());

        rafraichirButton = new Button("Rafraîchir", e -> loadTournois());

        actions.add(selectionnerButton, supprimerButton, rafraichirButton);
        add(actions);

        // Charger les données
        loadTournois();
    }

    private void updateTournoiActifInfo() {
        TournoiMulti actif = SessionInfo.getTournoiActif();
        if (actif != null) {
            tournoiActifInfo.setText("🎯 Tournoi actif : " + actif.getNom() + 
                                    " (ID: " + actif.getId() + 
                                    ", " + actif.getNbTerrains() + " terrains, " +
                                    actif.getNbJoueursParEquipe() + " joueurs/équipe)");
            tournoiActifInfo.getStyle()
                .set("background-color", "#e7f5ff")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #1976d2");
        } else {
            tournoiActifInfo.setText("⚠️ Aucun tournoi actif sélectionné. Sélectionnez-en un dans la liste ci-dessous.");
            tournoiActifInfo.getStyle()
                .set("background-color", "#fff3e0")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #ff9800");
        }
    }

    private void loadTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti> tournois = TournoiMulti.tousLesTournois(con);
            grid.setItems(tournois);

            if (tournois.isEmpty()) {
                Notification.show("ℹ️ Aucun tournoi créé. Créez-en un d'abord !", 
                                3000, Notification.Position.MIDDLE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur lors du chargement : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void selectionnerTournoi() {
        TournoiMulti selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("⚠️ Veuillez sélectionner un tournoi", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Définir ce tournoi comme actif dans la session
        SessionInfo.setTournoiActif(selected);

        Notification.show("✅ Tournoi actif : " + selected.getNom(), 
                        3000, Notification.Position.MIDDLE)
                   .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        // Mettre à jour l'affichage
        updateTournoiActifInfo();
    }

    private void supprimerTournoi() {
        TournoiMulti selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            selected.deleteFromDB(con);

            Notification.show("✅ Tournoi supprimé : " + selected.getNom(), 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Si le tournoi supprimé était actif, désélectionner
            if (SessionInfo.getTournoiActif() != null && 
                SessionInfo.getTournoiActif().getId() == selected.getId()) {
                SessionInfo.setTournoiActif(null);
                updateTournoiActifInfo();
            }

            loadTournois();

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur lors de la suppression : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}