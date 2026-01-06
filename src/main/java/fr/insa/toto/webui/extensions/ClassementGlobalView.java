package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// =====================================================
// VUE : Classement Global (Extension 2)
// =====================================================

@Route(value = "classement-global", layout = MainLayout.class)
@PageTitle("Classement Global")
class ClassementGlobalView extends VerticalLayout {

    public ClassementGlobalView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Classement Global de tous les tournois"));

        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti.ClassementGlobalInfo> classement = 
                TournoiMulti.getClassementGlobal(con);

            if (classement.isEmpty()) {
                add(new Paragraph("Aucune donnée de classement disponible."));
                return;
            }

            Grid<TournoiMulti.ClassementGlobalInfo> grid = 
                new Grid<>(TournoiMulti.ClassementGlobalInfo.class, false);
            
            grid.addColumn(c -> classement.indexOf(c) + 1).setHeader("Rang");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getSurnom).setHeader("Joueur");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNom).setHeader("Nom");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbTournois)
                .setHeader("Tournois");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbMatchs)
                .setHeader("Matchs");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbVictoires)
                .setHeader("Victoires");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getScoreTotal)
                .setHeader("Score Total");
            grid.addColumn(c -> String.format("%.1f%%", c.getTauxVictoire()))
                .setHeader("Taux victoire");

            grid.setItems(classement);
            add(grid);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}

// =====================================================
// VUE : Gestion des terrains avec plan (Extension 3)
// =====================================================

@Route(value = "terrains/gestion-plan", layout = MainLayout.class)
@PageTitle("Gestion des terrains")
class GestionTerrainsView extends VerticalLayout {

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

// =====================================================
// VUE : Gestion des types de jeu (Extensions 4-5)
// =====================================================

@Route(value = "types-jeu/gestion", layout = MainLayout.class)
@PageTitle("Gestion des types de jeu")
class GestionTypesJeuView extends VerticalLayout {

    private Grid<TypeJeu> grid;
    private FormLayout form;
    private TextField nomField;
    private IntegerField nbEquipesField;
    private IntegerField nbJoueursMinField;
    private IntegerField nbJoueursMaxField;
    private Button creer;

    public GestionTypesJeuView() {
        if (!SessionInfo.adminConnected()) {
            add(new Paragraph("Accès réservé aux administrateurs."));
            return;
        }

        setPadding(true);
        setSpacing(true);

        add(new H2("Gestion des types de jeu"));

        // Formulaire
        form = new FormLayout();
        nomField = new TextField("Nom du jeu");
        nbEquipesField = new IntegerField("Nombre d'équipes");
        nbEquipesField.setValue(2);
        nbEquipesField.setMin(2);

        nbJoueursMinField = new IntegerField("Joueurs min par équipe");
        nbJoueursMinField.setValue(1);
        nbJoueursMinField.setMin(1);

        nbJoueursMaxField = new IntegerField("Joueurs max par équipe");
        nbJoueursMaxField.setValue(1);
        nbJoueursMaxField.setMin(1);

        creer = new Button("Créer le type de jeu");
        creer.addClickListener(e -> creerTypeJeu());

        form.add(nomField, nbEquipesField, nbJoueursMinField, nbJoueursMaxField, creer);
        add(form);

        // Grille
        add(new H3("Types de jeu existants"));
        grid = new Grid<>(TypeJeu.class, false);
        grid.addColumn(TypeJeu::getNom).setHeader("Nom");
        grid.addColumn(TypeJeu::getNbEquipes).setHeader("Nb équipes");
        grid.addColumn(TypeJeu::getNbJoueursMin).setHeader("Joueurs min");
        grid.addColumn(TypeJeu::getNbJoueursMax).setHeader("Joueurs max");
        grid.addColumn(TypeJeu::isTailleEquipeVariable).setHeader("Taille variable");

        add(grid);
        chargerTypesJeu();
    }

    private void chargerTypesJeu() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TypeJeu> types = TypeJeu.tousLesTypesJeu(con);
            grid.setItems(types);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }

    private void creerTypeJeu() {
        String nom = nomField.getValue();
        Integer nbEquipes = nbEquipesField.getValue();
        Integer nbMin = nbJoueursMinField.getValue();
        Integer nbMax = nbJoueursMaxField.getValue();

        if (nom == null || nom.isBlank()) {
            Notification.show("Le nom est obligatoire");
            return;
        }
        if (nbEquipes == null || nbMin == null || nbMax == null) {
            Notification.show("Tous les champs sont obligatoires");
            return;
        }
        if (nbMin > nbMax) {
            Notification.show("Le min ne peut pas être supérieur au max");
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            TypeJeu tj = new TypeJeu(nom, nbEquipes, nbMin, nbMax);
            tj.saveInDB(con);
            Notification.show("Type de jeu créé !");
            nomField.clear();
            chargerTypesJeu();

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}

// =====================================================
// VUE : Gestion des templates (Extension 6)
// =====================================================

@Route(value = "templates/gestion", layout = MainLayout.class)
@PageTitle("Templates de tournoi")
class GestionTemplatesView extends VerticalLayout {

    private Grid<TemplateTournoi> grid;
    private FormLayout form;
    private TextField nomField;
    private TextField descriptionField;
    private IntegerField nbTerrainsField;
    private IntegerField nbJoueursField;
    private IntegerField dureeRondeField;
    private Checkbox publicField;
    private Button creer;
    private Button creerTournoiDepuisTemplate;

    public GestionTemplatesView() {
        if (!SessionInfo.adminConnected()) {
            add(new Paragraph("Accès réservé aux administrateurs."));
            return;
        }

        setPadding(true);
        setSpacing(true);

        add(new H2("Gestion des templates de tournoi"));

        // Formulaire
        form = new FormLayout();
        nomField = new TextField("Nom du template");
        descriptionField = new TextField("Description");
        nbTerrainsField = new IntegerField("Nb terrains");
        nbTerrainsField.setValue(2);
        nbJoueursField = new IntegerField("Joueurs par équipe");
        nbJoueursField.setValue(2);
        dureeRondeField = new IntegerField("Durée ronde (minutes)");
        dureeRondeField.setValue(20);
        publicField = new Checkbox("Template public");

        creer = new Button("Créer le template");
        creer.addClickListener(e -> creerTemplate());

        form.add(nomField, descriptionField, nbTerrainsField, nbJoueursField,
                dureeRondeField, publicField, creer);
        add(form);

        // Grille
        add(new H3("Templates existants"));
        grid = new Grid<>(TemplateTournoi.class, false);
        grid.addColumn(TemplateTournoi::getNom).setHeader("Nom");
        grid.addColumn(TemplateTournoi::getDescription).setHeader("Description");
        grid.addColumn(TemplateTournoi::getNbTerrains).setHeader("Terrains");
        grid.addColumn(TemplateTournoi::getNbJoueursParEquipe).setHeader("Joueurs/équipe");
        grid.addColumn(TemplateTournoi::getDureeRondeMinutes).setHeader("Durée (min)");
        grid.addColumn(t -> t.isPublic() ? "Public" : "Privé").setHeader("Visibilité");

        add(grid);

        // Bouton pour créer un tournoi depuis un template
        creerTournoiDepuisTemplate = new Button("Créer tournoi depuis sélection");
        creerTournoiDepuisTemplate.addClickListener(e -> creerTournoiFromTemplate());
        add(creerTournoiDepuisTemplate);

        chargerTemplates();
    }

    private void chargerTemplates() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TemplateTournoi> templates = TemplateTournoi.getTousLesTemplates(con);
            grid.setItems(templates);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }

    private void creerTemplate() {
        String nom = nomField.getValue();
        String description = descriptionField.getValue();
        Integer nbTerrains = nbTerrainsField.getValue();
        Integer nbJoueurs = nbJoueursField.getValue();
        Integer duree = dureeRondeField.getValue();
        boolean isPublic = publicField.getValue();

        if (nom == null || nom.isBlank()) {
            Notification.show("Le nom est obligatoire");
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            TemplateTournoi template = new TemplateTournoi(
                nom, description, nbTerrains, nbJoueurs, duree, isPublic
            );
            template.saveInDB(con);
            Notification.show("Template créé !");
            nomField.clear();
            descriptionField.clear();
            chargerTemplates();

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }

    private void creerTournoiFromTemplate() {
        TemplateTournoi selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("Sélectionnez un template");
            return;
        }

        // Dialog pour demander le nom du nouveau tournoi
        TextField nomTournoi = new TextField("Nom du nouveau tournoi");
        Button valider = new Button("Créer");

        VerticalLayout dialog = new VerticalLayout(
            new H3("Créer un tournoi depuis : " + selected.getNom()),
            nomTournoi,
            valider
        );

        valider.addClickListener(e -> {
            String nom = nomTournoi.getValue();
            if (nom == null || nom.isBlank()) {
                Notification.show("Le nom est obligatoire");
                return;
            }

            try (Connection con = ConnectionPool.getConnection()) {
                TournoiMulti nouveau = selected.creerTournoiDepuisTemplate(con, nom);
                Notification.show("Tournoi créé : " + nouveau.getNom());

            } catch (SQLException ex) {
                ex.printStackTrace();
                Notification.show("Erreur : " + ex.getMessage());
            }
        });

        // Note: Pour afficher un vrai dialog, il faudrait utiliser Dialog de Vaadin
        // Ici on montre la logique, mais l'UI complète nécessiterait Dialog component
        Notification.show("Fonctionnalité de dialog à compléter avec Vaadin Dialog");
    }
}

// =====================================================
// VUE : Liste des tournois (Extension 2)
// =====================================================

@Route(value = "tournois/liste", layout = MainLayout.class)
@PageTitle("Liste des tournois")
class ListeTournoisView extends VerticalLayout {

    private Grid<TournoiMulti> grid;

    public ListeTournoisView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Liste de tous les tournois"));

        grid = new Grid<>(TournoiMulti.class, false);
        grid.addColumn(TournoiMulti::getNom).setHeader("Nom");
        grid.addColumn(t -> t.getDateCreation().toLocalDate()).setHeader("Date création");
        grid.addColumn(t -> t.getDateDebut() != null 
            ? t.getDateDebut().toLocalDate() 
            : "Non démarré").setHeader("Date début");
        grid.addColumn(t -> t.getDateFin() != null 
            ? t.getDateFin().toLocalDate() 
            : "-").setHeader("Date fin");
        grid.addColumn(t -> t.getStatut().toString()).setHeader("Statut");
        grid.addColumn(TournoiMulti::getNbTerrains).setHeader("Nb terrains");

        add(grid);
        chargerTournois();
    }

    private void chargerTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti> tournois = TournoiMulti.tousLesTournois(con);
            grid.setItems(tournois);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}