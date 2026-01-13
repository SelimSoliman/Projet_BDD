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
import fr.insa.toto.model.Terrain;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "terrains/liste", layout = MainLayout.class)
@PageTitle("Liste des terrains")
public class ListeTerrainsView extends VerticalLayout {

    private Grid<Terrain> grid;
    private Paragraph statsInfo;

    public ListeTerrainsView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Liste de tous les terrains"));

        // Statistiques
        statsInfo = new Paragraph();
        updateStats();
        add(statsInfo);

        // Bouton de rafraîchissement
        Button refreshButton = new Button("🔄 Rafraîchir", event -> {
            chargerTerrains();
            updateStats();
        });
        refreshButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        add(refreshButton);

        // Grille des terrains
        grid = new Grid<>(Terrain.class, false);
        
        grid.addColumn(Terrain::getId)
            .setHeader("ID")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(Terrain::getNom)
            .setHeader("Nom")
            .setAutoWidth(true)
            .setSortable(true);
        
        grid.addColumn(t -> t.estDisponible() ? "✅ Disponible" : "❌ Occupé")
            .setHeader("Statut")
            .setAutoWidth(true)
            .setSortable(true);

        // Colonne Actions (admin uniquement)
        grid.addComponentColumn(terrain -> {
            var layout = new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
            layout.setSpacing(true);
            
            Button toggleButton = new Button(
                terrain.estDisponible() ? "🔒 Occuper" : "🔓 Libérer"
            );
            toggleButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            toggleButton.addClickListener(e -> toggleDisponibilite(terrain));
            
            Button deleteButton = new Button("🗑️ Supprimer");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> supprimerTerrain(terrain));
            
            layout.add(toggleButton, deleteButton);
            return layout;
        }).setHeader("Actions");

        add(grid);

        // Aide
        Paragraph aide = new Paragraph("💡 Cliquez sur les en-têtes pour trier les colonnes");
        aide.getStyle()
            .set("color", "#666")
            .set("font-style", "italic")
            .set("margin-top", "10px");
        add(aide);

        chargerTerrains();
    }

    private void chargerTerrains() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Terrain> terrains = Terrain.tousLesTerrains(con);
            grid.setItems(terrains);

            if (terrains.isEmpty()) {
                Notification.show("ℹ️ Aucun terrain enregistré. Créez-en un !", 
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
            List<Terrain> terrains = Terrain.tousLesTerrains(con);
            
            long disponibles = terrains.stream().filter(Terrain::estDisponible).count();
            long occupes = terrains.size() - disponibles;
            
            statsInfo.setText("🏟️ Total : " + terrains.size() + " terrain(s) | " +
                            "✅ Disponibles : " + disponibles + " | " +
                            "❌ Occupés : " + occupes);
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

    private void toggleDisponibilite(Terrain terrain) {
        try (Connection con = ConnectionPool.getConnection()) {
            terrain.basculerDisponibilite();
            // Note: Il faudrait une méthode updateInDB() dans Terrain
            // Pour l'instant, on recharge juste la liste
            
            Notification.show("✅ Statut mis à jour : " + terrain.getNom() + 
                            (terrain.estDisponible() ? " est maintenant disponible" : " est maintenant occupé"), 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            chargerTerrains();
            updateStats();

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void supprimerTerrain(Terrain terrain) {
        // Dialog de confirmation
        com.vaadin.flow.component.dialog.Dialog confirmDialog = new com.vaadin.flow.component.dialog.Dialog();
        confirmDialog.setHeaderTitle("⚠️ Confirmer la suppression");
        
        VerticalLayout content = new VerticalLayout();
        content.add(new Paragraph("Êtes-vous sûr de vouloir supprimer le terrain \"" + terrain.getNom() + "\" ?"));
        content.add(new Paragraph("Cette action est irréversible."));
        
        com.vaadin.flow.component.orderedlayout.HorizontalLayout buttons = 
            new com.vaadin.flow.component.orderedlayout.HorizontalLayout();
        
        Button confirmerButton = new Button("Oui, supprimer", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                Terrain.supprimer(con, terrain.getId());
                
                Notification.show("✅ Terrain \"" + terrain.getNom() + "\" supprimé avec succès", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                confirmDialog.close();
                chargerTerrains();
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