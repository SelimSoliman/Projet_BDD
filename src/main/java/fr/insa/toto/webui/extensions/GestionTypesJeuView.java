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
@Route(value = "types-jeu", layout = MainLayout.class)
@PageTitle("Types de jeu")


public class GestionTypesJeuView extends VerticalLayout {

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
