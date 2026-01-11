package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;

@Route(value = "terrains/creer", layout = MainLayout.class)
@PageTitle("Créer un terrain")
public class CreationTerrainView extends VerticalLayout {

    private TextField nomField;
    private Checkbox disponibleCheckbox;
    private Button creerButton;
    private Button annulerButton;

    public CreationTerrainView() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add("Seuls les administrateurs peuvent créer des terrains.");
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        add(new H2("Créer un nouveau terrain"));

        // Formulaire
        FormLayout form = new FormLayout();

        nomField = new TextField("Nom du terrain");
        nomField.setPlaceholder("Ex: Court Central, Terrain A...");
        nomField.setRequired(true);
        nomField.setWidthFull();

        disponibleCheckbox = new Checkbox("Disponible immédiatement");
        disponibleCheckbox.setValue(true);
        disponibleCheckbox.setHelperText("Décochez si le terrain n'est pas encore prêt");

        form.add(nomField, disponibleCheckbox);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        add(form);

        // Boutons
        creerButton = new Button("✅ Créer le terrain", e -> creerTerrain());
        creerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        annulerButton = new Button("🔄 Réinitialiser", e -> {
            nomField.clear();
            disponibleCheckbox.setValue(true);
            Notification.show("Formulaire réinitialisé");
        });

        add(new com.vaadin.flow.component.orderedlayout.HorizontalLayout(
            creerButton, annulerButton
        ));
    }

    private void creerTerrain() {
        String nom = nomField.getValue();
        boolean disponible = disponibleCheckbox.getValue();

        // Validation
        if (nom == null || nom.isBlank()) {
            Notification.show("⚠️ Le nom du terrain est obligatoire", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Création du terrain
        try (Connection con = ConnectionPool.getConnection()) {
            Terrain terrain = new Terrain(nom);
            terrain.setDisponible(disponible);
            terrain.saveInDB(con);

            Notification.show("✅ Terrain créé : " + nom + 
                            (disponible ? " (disponible)" : " (non disponible)"), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Réinitialiser le formulaire
            nomField.clear();
            disponibleCheckbox.setValue(true);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur lors de la création : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}