

package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD; // adapte si ton package est différent
import fr.insa.toto.model.Tournoi;

import java.sql.Connection;
import java.sql.SQLException;

@Route(value = "tournoi", layout = MainLayout.class)
@PageTitle("Paramètres du tournoi")
public class TournoiParamView extends VerticalLayout {

    private Connection con;

    private final TextField nom = new TextField("Nom du tournoi");
    private final IntegerField nbTerrains = new IntegerField("Nombre de terrains");
    private final IntegerField nbJoueursParEquipe = new IntegerField("Joueurs par équipe");

    private final Button enregistrer = new Button("Enregistrer");
    private final Button recharger = new Button("Recharger");

    private Tournoi tournoi;

    public TournoiParamView() {
        // 1) Connexion BDD (ne jamais throws dans un constructeur de View Vaadin)
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
        } catch (SQLException e) {
            Notification.show("Impossible de se connecter à la base : " + e.getMessage());
            e.printStackTrace();
            // on stoppe proprement : la vue s'affiche mais sans fonctionnalités
            setEnabled(false);
            return;
        }

        // 2) UI
        setMaxWidth("900px");
        setWidthFull();

        nbTerrains.setMin(1);
        nbJoueursParEquipe.setMin(1);

        add(
                new H2("Paramètres du tournoi"),
                nom, nbTerrains, nbJoueursParEquipe,
                new HorizontalLayout(enregistrer, recharger)
        );

        // 3) Listeners (gérer les erreurs sans RuntimeException brutale)
        recharger.addClickListener(e -> {
            try {
                load();
            } catch (SQLException ex) {
                Notification.show("Erreur recharge : " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        enregistrer.addClickListener(e -> {
            try {
                save();
            } catch (SQLException ex) {
                Notification.show("Erreur sauvegarde : " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // 4) Chargement initial
        try {
            load();
        } catch (SQLException ex) {
            Notification.show("Erreur chargement : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void load() throws SQLException {
        tournoi = Tournoi.getUnique(con);

        if (tournoi == null) {
            nom.clear();
            nbTerrains.clear();
            nbJoueursParEquipe.setValue(2); // valeur par défaut
        } else {
            nom.setValue(tournoi.getNom());
            nbTerrains.setValue(tournoi.getNbTerrains());
            nbJoueursParEquipe.setValue(tournoi.getNbJoueursParEquipe());
        }
    }

    private void save() throws SQLException {
        if (nom.getValue() == null || nom.getValue().isBlank()) {
            Notification.show("Nom obligatoire");
            return;
        }
        if (nbTerrains.getValue() == null || nbTerrains.getValue() < 1) {
            Notification.show("Nombre de terrains invalide");
            return;
        }
        if (nbJoueursParEquipe.getValue() == null || nbJoueursParEquipe.getValue() < 1) {
            Notification.show("Joueurs par équipe invalide");
            return;
        }

        if (tournoi == null) {
            tournoi = new Tournoi(nom.getValue(), nbTerrains.getValue());
            tournoi.setNbJoueursParEquipe(nbJoueursParEquipe.getValue());
            tournoi.saveInDB(con);
        } else {
            tournoi.setNom(nom.getValue());
            tournoi.setNbTerrains(nbTerrains.getValue());
            tournoi.setNbJoueursParEquipe(nbJoueursParEquipe.getValue());
            tournoi.updateInDB(con);
        }

        Notification.show("Paramètres enregistrés !");
        load();
    }
}
