package fr.insa.toto.webui.tournois;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;

@Route(value = "tournois/creer", layout = MainLayout.class)
@PageTitle("Créer un tournoi")
public class CreerTournoiView extends VerticalLayout {

    private TextField nomField;
    private IntegerField nbTerrainsField;
    private IntegerField nbJoueursParEquipeField;
    private Button creerButton;
    private Button annulerButton;

    public CreerTournoiView() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add("Seuls les administrateurs peuvent créer des tournois.");
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        add(new H2("Créer un nouveau tournoi"));

        // Formulaire
        FormLayout form = new FormLayout();

        nomField = new TextField("Nom du tournoi");
        nomField.setPlaceholder("Ex: Tournoi Printemps 2025");
        nomField.setRequired(true);
        nomField.setWidthFull();

        nbTerrainsField = new IntegerField("Nombre de terrains");
        nbTerrainsField.setValue(4);
        nbTerrainsField.setMin(1);
        nbTerrainsField.setMax(20);
        nbTerrainsField.setHelperText("Terrains disponibles pour les matchs");

        nbJoueursParEquipeField = new IntegerField("Joueurs par équipe");
        nbJoueursParEquipeField.setValue(2);
        nbJoueursParEquipeField.setMin(1);
        nbJoueursParEquipeField.setMax(11);
        nbJoueursParEquipeField.setHelperText("Nombre de joueurs dans chaque équipe");

        form.add(nomField, nbTerrainsField, nbJoueursParEquipeField);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        add(form);

        // Boutons
        creerButton = new Button("Créer le tournoi", e -> creerTournoi());
        creerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        annulerButton = new Button("Annuler", e -> {
            nomField.clear();
            nbTerrainsField.setValue(4);
            nbJoueursParEquipeField.setValue(2);
            Notification.show("Formulaire réinitialisé");
        });

        add(new com.vaadin.flow.component.orderedlayout.HorizontalLayout(
            creerButton, annulerButton
        ));
    }

    private void creerTournoi() {
        String nom = nomField.getValue();
        Integer nbTerrains = nbTerrainsField.getValue();
        Integer nbJoueurs = nbJoueursParEquipeField.getValue();

        // Validation
        if (nom == null || nom.isBlank()) {
            Notification.show("⚠️ Le nom du tournoi est obligatoire", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (nbTerrains == null || nbTerrains < 1) {
            Notification.show("⚠️ Le nombre de terrains doit être au moins 1", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (nbJoueurs == null || nbJoueurs < 1) {
            Notification.show("⚠️ Le nombre de joueurs par équipe doit être au moins 1", 
                            3000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Création du tournoi
        try (Connection con = ConnectionPool.getConnection()) {
            TournoiMulti tournoi = new TournoiMulti(nom, nbTerrains, nbJoueurs);
            tournoi.saveInDB(con);

            Notification.show("✅ Tournoi créé : " + nom, 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Réinitialiser le formulaire
            nomField.clear();
            nbTerrainsField.setValue(4);
            nbJoueursParEquipeField.setValue(2);

            // Optionnel : naviguer vers la liste des tournois
            getUI().ifPresent(ui -> ui.navigate(ListeTournoisView.class));

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur lors de la création : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}