/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.toto.webui.utilisateurs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author ThinkPad
 */
@Route(value = "utilisateurs/liste", layout = MainLayout.class)
@PageTitle("Teqball")
public class ListeUtilisateurs extends VerticalLayout {
    
    private Grid<Utilisateur> grid;
    
    public ListeUtilisateurs() {
        this.add(new H2("Liste de tous les utilisateurs"));

        grid = new Grid<>(Utilisateur.class, false);
        grid.addColumn(Utilisateur::getSurnom).setHeader("surnom");
        grid.addColumn(Utilisateur::getRole).setHeader("roleID");
        grid.addColumn(u -> u.getRole() == 1 ? "admin" : "utilisateur").setHeader("role");
        grid.addColumn(new ComponentRenderer<Icon, Utilisateur>(u -> {
            return u.getRole() == 1
                    ? VaadinIcon.THUMBS_UP.create()
                    : VaadinIcon.THUMBS_DOWN.create();
        })).setHeader("admin?");

        // Colonne Actions avec bouton de suppression
        grid.addComponentColumn(utilisateur -> {
            Button deleteButton = new Button("🗑️ Supprimer");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> supprimerUtilisateur(utilisateur));
            
            // Protection : ne pas permettre de supprimer l'utilisateur actuellement connecté
            try {
                Utilisateur userConnecte = SessionInfo.userConnected();  // ✅ CORRIGÉ
                if (userConnecte != null && utilisateur.getId() == userConnecte.getId()) {
                    deleteButton.setEnabled(false);
                    deleteButton.setText("⚠️ Vous");
                    deleteButton.getElement().setAttribute("title", "Vous ne pouvez pas vous supprimer vous-même");
                }
            } catch (Exception ex) {
                // Si erreur, on laisse le bouton actif (mieux que de planter)
            }
            
            return deleteButton;
        }).setHeader("Actions");

        chargerUtilisateurs();
        this.add(grid);
    }

    private void chargerUtilisateurs() {
        try (Connection con = ConnectionPool.getConnection()) {
            grid.setItems(Utilisateur.tousLesUtilisateur(con));
        } catch (SQLException ex) {
            Notification.show("Problème : " + ex.getLocalizedMessage());
            System.getLogger(ListeUtilisateurs.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    /**
     * Supprime un utilisateur après confirmation
     */
    private void supprimerUtilisateur(Utilisateur utilisateur) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("⚠️ Confirmer la suppression");
        
        VerticalLayout content = new VerticalLayout();
        content.add(new Paragraph("Êtes-vous sûr de vouloir supprimer l'utilisateur \"" + 
                                  utilisateur.getSurnom() + "\" ?"));
        content.add(new Paragraph("Cette action est irréversible."));
        
        HorizontalLayout buttons = new HorizontalLayout();
        
        Button confirmerButton = new Button("Oui, supprimer", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                Utilisateur.supprimer(con, utilisateur.getId());
                
                Notification.show("✅ Utilisateur \"" + utilisateur.getSurnom() + "\" supprimé avec succès", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                confirmDialog.close();
                chargerUtilisateurs();
                
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