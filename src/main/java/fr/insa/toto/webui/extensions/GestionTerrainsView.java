
package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.session.SessionInfo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


public class GestionTerrainsView extends VerticalLayout {

    private Grid<TerrainAvecPlan> grid;
    private FormLayout formTerrain;
    private TextField nomField;
    private TextField descriptionField;
    private Upload uploadPlan;
    private Button sauvegarder;

    public GestionTerrainsView() {
        if (!SessionInfo.adminConnected()) {
            add(new Paragraph("Accès réservé aux administrateurs."));
            return;
        }

        setPadding(true);
        setSpacing(true);

        add(new H2("Gestion des terrains avec plan"));

        // Formulaire
        formTerrain = new FormLayout();
        nomField = new TextField("Nom du terrain");
        descriptionField = new TextField("Description");
        
        MemoryBuffer buffer = new MemoryBuffer();
        uploadPlan = new Upload(buffer);
        uploadPlan.setAcceptedFileTypes("image/*", "application/pdf");
        uploadPlan.setMaxFiles(1);

        sauvegarder = new Button("Enregistrer");
        sauvegarder.addClickListener(e -> sauvegarderTerrain(buffer));

        formTerrain.add(nomField, descriptionField, uploadPlan, sauvegarder);
        add(formTerrain);

        // Grille des terrains
        add(new H3("Terrains existants"));
        grid = new Grid<>(TerrainAvecPlan.class, false);
        grid.addColumn(TerrainAvecPlan::getId).setHeader("ID");
        grid.addColumn(TerrainAvecPlan::getNom).setHeader("Nom");
        grid.addColumn(TerrainAvecPlan::getDescription).setHeader("Description");
        grid.addColumn(t -> t.getCheminPlan() != null ? "Oui" : "Non")
            .setHeader("Plan disponible");
        grid.addColumn(t -> t.estDisponible() ? "Disponible" : "Occupé")
            .setHeader("Statut");

        // Affichage du plan au clic
        grid.addItemClickListener(event -> {
            TerrainAvecPlan t = event.getItem();
            if (t.getCheminPlan() != null) {
                afficherPlan(t);
            } else {
                Notification.show("Aucun plan disponible pour ce terrain");
            }
        });

        add(grid);
        chargerTerrains();
    }

    private void chargerTerrains() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TerrainAvecPlan> terrains = TerrainAvecPlan.tousLesTerrainsAvecPlan(con);
            grid.setItems(terrains);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur de chargement : " + ex.getMessage());
        }
    }

    private void sauvegarderTerrain(MemoryBuffer buffer) {
        String nom = nomField.getValue();
        String description = descriptionField.getValue();

        if (nom == null || nom.isBlank()) {
            Notification.show("Le nom est obligatoire");
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            TerrainAvecPlan terrain = new TerrainAvecPlan(nom, description);
            terrain.saveInDB(con);

            // Upload du fichier
            InputStream inputStream = buffer.getInputStream();
            if (inputStream != null) {
                String fileName = buffer.getFileName();
                String extension = fileName.substring(fileName.lastIndexOf("."));
                String cheminPlan = "terrains/" + terrain.getId() + extension;

                // Créer le dossier si nécessaire
                Path dossier = Path.of("uploads/terrains");
                Files.createDirectories(dossier);

                // Copier le fichier
                Path destination = dossier.resolve(terrain.getId() + extension);
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);

                // Mettre à jour en BD
                terrain.updatePlan(con, cheminPlan);
            }

            Notification.show("Terrain enregistré !");
            nomField.clear();
            descriptionField.clear();
            chargerTerrains();

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }

    private void afficherPlan(TerrainAvecPlan terrain) {
        VerticalLayout dialog = new VerticalLayout();
        dialog.add(new H3("Plan : " + terrain.getNom()));

        String chemin = terrain.getCheminPlan();
        if (chemin.endsWith(".pdf")) {
            dialog.add(new Paragraph("Fichier PDF - Téléchargez le plan pour le visualiser"));
            // Possibilité d'ajouter un lien de téléchargement
        } else {
            Image img = new Image("uploads/" + chemin, "Plan du terrain");
            img.setMaxWidth("800px");
            dialog.add(img);
        }

        // Afficher dans une notification ou un dialog
        Notification.show(terrain.getNom() + " : voir le plan dans les fichiers uploadés");
    }
}
