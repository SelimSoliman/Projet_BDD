package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

@Route(value = "tournois", layout = MainLayout.class)
@PageTitle("Liste des tournois")
public class ListeTournoisView extends VerticalLayout {

    private Grid<TournoiMulti> grid;
    private Paragraph tournoiActifInfo;

    public ListeTournoisView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Liste de tous les tournois"));

        // ✅ Afficher le tournoi actuellement actif
        tournoiActifInfo = new Paragraph();
        updateTournoiActifInfo();
        add(tournoiActifInfo);

        // ✅ Grille des tournois
        grid = new Grid<>(TournoiMulti.class, false);
        grid.addColumn(TournoiMulti::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(TournoiMulti::getNom).setHeader("Nom").setAutoWidth(true);
        grid.addColumn(t -> t.getDateCreation().toLocalDate()).setHeader("Date création").setAutoWidth(true);
        grid.addColumn(t -> t.getDateDebut() != null 
            ? t.getDateDebut().toLocalDate() 
            : "Non démarré").setHeader("Date début").setAutoWidth(true);
        grid.addColumn(t -> t.getDateFin() != null 
            ? t.getDateFin().toLocalDate() 
            : "-").setHeader("Date fin").setAutoWidth(true);
        grid.addColumn(t -> t.getStatut().toString()).setHeader("Statut").setAutoWidth(true);
        grid.addColumn(TournoiMulti::getNbTerrains).setHeader("Nb terrains").setAutoWidth(true);
        grid.addColumn(TournoiMulti::getNbJoueursParEquipe).setHeader("Joueurs/équipe").setAutoWidth(true);
        
        // ✅ Colonne Actions avec bouton de suppression (ADMIN UNIQUEMENT)
        grid.addComponentColumn(tournoi -> {
            // ✅ CORRECTION : Vérifier si l'utilisateur est admin
            if (!SessionInfo.adminConnected()) {
                // Si ce n'est pas un admin, ne rien afficher
                return new Paragraph("");
            }
            
            Button deleteButton = new Button("🗑️ Supprimer");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> supprimerTournoi(tournoi));
            return deleteButton;
        }).setHeader("Actions");

        // ✅ SOLUTION 1 : Clic sur ligne = sélection automatique
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addItemClickListener(event -> {
            TournoiMulti clicked = event.getItem();
            if (clicked != null) {
                definirTournoiActif(clicked);
            }
        });

        add(grid);

        // ✅ Message d'aide
        Paragraph aide = new Paragraph("💡 Cliquez sur un tournoi pour le sélectionner comme tournoi actif");
        aide.getStyle()
            .set("color", "#666")
            .set("font-style", "italic")
            .set("margin-top", "10px");
        add(aide);

        chargerTournois();
    }

    private void chargerTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti> tournois = TournoiMulti.tousLesTournois(con);
            grid.setItems(tournois);

            if (tournois.isEmpty()) {
                Notification.show("ℹ️ Aucun tournoi créé. Créez d'abord un tournoi.", 
                                4000, Notification.Position.MIDDLE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * ✅ Définit un tournoi comme actif
     */
    private void definirTournoiActif(TournoiMulti tournoi) {
        if (tournoi == null) {
            return;
        }

        // Définir le tournoi actif dans la session
        SessionInfo.setTournoiActif(tournoi);

        Notification notification = Notification.show(
            "✅ Tournoi actif : " + tournoi.getNom(), 
            3000, 
            Notification.Position.MIDDLE
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        // Mettre à jour l'affichage
        updateTournoiActifInfo();
    }

    /**
     * ✅ Met à jour l'affichage du tournoi actif
     */
    private void updateTournoiActifInfo() {
        TournoiMulti actif = SessionInfo.getTournoiActif();
        
        if (actif == null) {
            tournoiActifInfo.setText("⚠️ Aucun tournoi actif sélectionné");
            tournoiActifInfo.getStyle()
                .set("background-color", "#fff3e0")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #ff9800")
                .set("font-weight", "bold");
        } else {
            tournoiActifInfo.setText("🎯 Tournoi actif : " + actif.getNom() + " (ID: " + actif.getId() + ")");
            tournoiActifInfo.getStyle()
                .set("background-color", "#e7f5ff")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #1976d2")
                .set("font-weight", "bold");
        }
    }
    
    /**
     * Supprime un tournoi après confirmation (ADMIN UNIQUEMENT)
     */
    private void supprimerTournoi(TournoiMulti tournoi) {
        // ✅ DOUBLE VÉRIFICATION : Sécurité côté serveur
        if (!SessionInfo.adminConnected()) {
            Notification.show("❌ Accès refusé : seuls les administrateurs peuvent supprimer des tournois", 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        com.vaadin.flow.component.dialog.Dialog confirmDialog = new com.vaadin.flow.component.dialog.Dialog();
        confirmDialog.setHeaderTitle("⚠️ Confirmer la suppression");
        
        VerticalLayout content = new VerticalLayout();
        content.add(new Paragraph("Êtes-vous sûr de vouloir supprimer le tournoi \"" + tournoi.getNom() + "\" ?"));
        content.add(new Paragraph("⚠️ ATTENTION : Cela supprimera aussi toutes les rondes et matchs associés."));
        content.add(new Paragraph("Cette action est irréversible."));
        
        com.vaadin.flow.component.orderedlayout.HorizontalLayout buttons = 
            new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
        
        Button confirmerButton = new Button("Oui, supprimer", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                // Supprimer le tournoi (cascade supprime rondes et matchs)
                TournoiMulti.supprimer(con, tournoi.getId());
                
                Notification.show("✅ Tournoi \"" + tournoi.getNom() + "\" supprimé avec succès", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                // Si c'était le tournoi actif, le retirer de la session
                if (SessionInfo.getTournoiActif() != null && 
                    SessionInfo.getTournoiActif().getId() == tournoi.getId()) {
                    SessionInfo.setTournoiActif(null);
                }
                
                confirmDialog.close();
                chargerTournois();
                updateTournoiActifInfo();
                
            } catch (SQLException ex) {
                ex.printStackTrace();
                Notification.show("❌ Erreur lors de la suppression : " + ex.getMessage(), 
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
}