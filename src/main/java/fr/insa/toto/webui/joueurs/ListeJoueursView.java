package fr.insa.toto.webui.joueurs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
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
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Route(value = "joueurs/liste", layout = MainLayout.class)
@PageTitle("Liste des joueurs")
public class ListeJoueursView extends VerticalLayout {

    private Grid<Joueur> grid;
    private Paragraph statsInfo;

    public ListeJoueursView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Liste de tous les joueurs"));

        // Statistiques
        statsInfo = new Paragraph();
        updateStats();
        add(statsInfo);

        // Bouton de rafraîchissement
        Button refreshButton = new Button("🔄 Rafraîchir", event -> {
            chargerJoueurs();
            updateStats();
        });
        refreshButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        add(refreshButton);

        // Grille des joueurs
        grid = new Grid<>(Joueur.class, false);
        
        grid.addColumn(Joueur::getId)
            .setHeader("ID")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(Joueur::getSurnom)
            .setHeader("Surnom")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(Joueur::getNom)
            .setHeader("Nom")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(Joueur::getPrenom)
            .setHeader("Prénom")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(Joueur::getSexe)
            .setHeader("Sexe")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(Joueur::getCategorie)
            .setHeader("Catégorie")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(Joueur::getTaillecm)
            .setHeader("Taille (cm)")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(j -> {
            LocalDate dateNaissance = j.getDateNaissance();
            if (dateNaissance == null) return "-";
            return dateNaissance.toString();
        })
            .setHeader("Date naissance")
            .setAutoWidth(true)
            .setSortable(true);
        
        // Colonne calculée : Âge
        grid.addColumn(j -> {
            LocalDate dateNaissance = j.getDateNaissance();
            if (dateNaissance == null) return "-";
            int age = Period.between(dateNaissance, LocalDate.now()).getYears();
            return age + " ans";
        })
            .setHeader("Âge")
            .setAutoWidth(true);

        // Colonne Actions avec boutons
        grid.addComponentColumn(joueur -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            // Bouton "Voir détails"
            Button detailButton = new Button("👁️ Détails");
            detailButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            detailButton.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.navigate("joueurs/detail/" + joueur.getId()));
            });
            
            // Bouton "Supprimer"
            Button deleteButton = new Button("🗑️");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> supprimerJoueur(joueur));
            
            actions.add(detailButton, deleteButton);
            actions.setSpacing(true);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        // Rendre les lignes cliquables
        grid.addItemClickListener(event -> {
            Joueur joueur = event.getItem();
            getUI().ifPresent(ui -> ui.navigate("joueurs/detail/" + joueur.getId()));
        });

        // Style pour indiquer que les lignes sont cliquables
        grid.addClassName("clickable-grid");
        
        add(grid);

        // Aide
        Paragraph aide = new Paragraph("💡 Cliquez sur un joueur pour voir ses détails et matchs, ou sur les boutons d'action");
        aide.getStyle()
            .set("color", "#666")
            .set("font-style", "italic")
            .set("margin-top", "10px");
        add(aide);

        chargerJoueurs();
    }

    private void chargerJoueurs() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            grid.setItems(joueurs);

            if (joueurs.isEmpty()) {
                Notification.show("ℹ️ Aucun joueur enregistré. Créez-en un !", 
                                4000, Notification.Position.MIDDLE);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateStats() {
        try (Connection con = ConnectionPool.getConnection()) {
            int nbJoueurs = Joueur.count(con);
            
            statsInfo.setText("👥 Total : " + nbJoueurs + " joueur(s) enregistré(s)");
            statsInfo.getStyle()
                .set("background-color", "#e7f5ff")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #1976d2")
                .set("font-weight", "bold");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Supprime un joueur après confirmation
     */
    private void supprimerJoueur(Joueur joueur) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("⚠️ Confirmer la suppression");
        
        VerticalLayout content = new VerticalLayout();
        content.add(new Paragraph("Êtes-vous sûr de vouloir supprimer le joueur \"" + 
                                  joueur.getSurnom() + "\" (" + joueur.getNom() + " " + joueur.getPrenom() + ") ?"));
        content.add(new Paragraph("Cette action est irréversible."));
        
        HorizontalLayout buttons = new HorizontalLayout();
        
        Button confirmerButton = new Button("Oui, supprimer", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                Joueur.supprimer(con, joueur.getId());
                
                Notification.show("✅ Joueur \"" + joueur.getSurnom() + "\" supprimé avec succès", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                confirmDialog.close();
                chargerJoueurs();
                updateStats();
                
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