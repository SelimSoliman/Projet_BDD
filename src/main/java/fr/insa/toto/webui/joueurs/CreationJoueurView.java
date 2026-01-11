package fr.insa.toto.webui.joueurs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;

@Route(value = "joueurs/creer", layout = MainLayout.class)
@PageTitle("Créer un joueur")
public class CreationJoueurView extends VerticalLayout {

    private TextField surnomField;
    private TextField categorieField;
    private IntegerField taillecmField;
    private TextField nomField;
    private TextField prenomField;
    private ComboBox<String> sexeCombo;
    private DatePicker dateNaissancePicker;

    public CreationJoueurView() {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        add(new H2("Créer un nouveau joueur"));

        // Formulaire
        FormLayout formLayout = new FormLayout();

        // ========== Champs principaux ==========
        surnomField = new TextField("Surnom");
        surnomField.setRequired(true);
        surnomField.setPlaceholder("Ex: TheBeast");
        surnomField.setHelperText("Identifiant unique du joueur");

        categorieField = new TextField("Catégorie");
        categorieField.setPlaceholder("Ex: Senior, Junior, Expert...");
        categorieField.setHelperText("Niveau ou catégorie du joueur");

        taillecmField = new IntegerField("Taille (cm)");
        taillecmField.setPlaceholder("Ex: 175");
        taillecmField.setMin(100);
        taillecmField.setMax(250);
        taillecmField.setHelperText("Taille en centimètres");

        // ========== Informations détaillées ==========
        nomField = new TextField("Nom");
        nomField.setPlaceholder("Ex: Dupont");

        prenomField = new TextField("Prénom");
        prenomField.setPlaceholder("Ex: Jean");

        sexeCombo = new ComboBox<>("Sexe");
        sexeCombo.setItems("M", "F", "Autre");
        sexeCombo.setPlaceholder("Choisir...");

        dateNaissancePicker = new DatePicker("Date de naissance");
        dateNaissancePicker.setLocale(Locale.FRANCE);
        dateNaissancePicker.setMax(LocalDate.now()); // Pas de date future
        dateNaissancePicker.setPlaceholder("jj/mm/aaaa");

        // Ajout au formulaire (2 colonnes)
        formLayout.add(surnomField, categorieField);
        formLayout.add(taillecmField, sexeCombo);
        formLayout.add(nomField, prenomField);
        formLayout.add(dateNaissancePicker);

        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        add(formLayout);

        // Boutons
        Button creerButton = new Button("✅ Créer le joueur", event -> creerJoueur());
        creerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button resetButton = new Button("🔄 Réinitialiser", event -> resetForm());

        add(creerButton, resetButton);
    }

    private void creerJoueur() {
        // Récupération des valeurs
        String surnom = surnomField.getValue() != null ? surnomField.getValue().trim() : "";
        String categorie = categorieField.getValue() != null ? categorieField.getValue().trim() : "";
        Integer taillecm = taillecmField.getValue();
        String nom = nomField.getValue() != null ? nomField.getValue().trim() : "";
        String prenom = prenomField.getValue() != null ? prenomField.getValue().trim() : "";
        String sexe = sexeCombo.getValue();
        LocalDate dateNaissance = dateNaissancePicker.getValue();

        // ========== Validation ==========
        if (surnom.isEmpty()) {
            Notification.show("⚠️ Le surnom est obligatoire", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (taillecm == null || taillecm <= 0) {
            Notification.show("⚠️ La taille doit être un nombre positif", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (dateNaissance == null) {
            Notification.show("⚠️ La date de naissance est obligatoire", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            // Créer le joueur
            Joueur nouveauJoueur = new Joueur(
                surnom, 
                categorie.isEmpty() ? "Non définie" : categorie, 
                taillecm,
                nom.isEmpty() ? "?" : nom,
                prenom.isEmpty() ? "?" : prenom,
                sexe != null ? sexe : "?",
                dateNaissance
            );

            // Sauvegarder dans la BDD
            nouveauJoueur.saveInDB(con);

            Notification notification = Notification.show(
                "✅ Joueur créé avec succès : " + nouveauJoueur.getSurnom() + " (ID: " + nouveauJoueur.getId() + ")",
                4000,
                Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            resetForm();

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur lors de la création : " + ex.getMessage(),
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void resetForm() {
        surnomField.clear();
        categorieField.clear();
        taillecmField.clear();
        nomField.clear();
        prenomField.clear();
        sexeCombo.clear();
        dateNaissancePicker.clear();
    }
}