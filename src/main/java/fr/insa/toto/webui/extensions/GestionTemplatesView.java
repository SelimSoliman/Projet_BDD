package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "templates", layout = MainLayout.class)
@PageTitle("Gestion des templates")

public class GestionTemplatesView extends VerticalLayout {

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