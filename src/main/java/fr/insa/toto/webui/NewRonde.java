package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.*;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "rondes/new", layout = MainLayout.class)
@PageTitle("Créer une ronde")
public class NewRonde extends VerticalLayout {

    public NewRonde() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Créer une nouvelle ronde"));

        Button creerRonde = new Button("Créer la ronde + générer les matchs");
        add(creerRonde);

        creerRonde.addClickListener(e -> creerNouvelleRonde());
    }

    private void creerNouvelleRonde() {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {

            // 1) tournoi unique
            Tournoi tournoi = Tournoi.getTournoiUnique(con);
            if (tournoi == null) {
                Notification.show("Aucun tournoi n'existe encore.", 4000, Notification.Position.MIDDLE);
                return;
            }

            // 2) dernière ronde
           Ronde derniere = Ronde.findDerniereRonde(con, tournoi);

            if (derniere != null && !derniere.isClose()) {
                Notification.show("Impossible : la ronde " + derniere.getNumero() + " est encore en cours.", 4000,
                        Notification.Position.MIDDLE);
                return;
            }

            // 3) créer ronde
            int numero = (derniere == null) ? 1 : derniere.getNumero() + 1;
            Ronde nouvelle = new Ronde(tournoi, numero);
            nouvelle.saveInDB(con);

            // 4) charger joueurs + terrains depuis la base
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            joueurs.forEach(tournoi::ajouterJoueur);

            List<Terrain> terrainsDispo = Terrain.tousLesTerrains(con).stream()
                    .filter(Terrain::estDisponible)
                    .limit(tournoi.getNbTerrains())          // ✅ on ne prend que nbTerrains
                    .collect(Collectors.toList());
            terrainsDispo.forEach(tournoi::ajouterTerrain);

            // 5) générer matchs
            int tailleEquipe = tournoi.getNbJoueursParEquipe();
            tournoi.creerMatchsPourRonde(nouvelle, tailleEquipe, con);

            Notification.show("Ronde " + numero + " créée + matchs générés ✅", 4000, Notification.Position.MIDDLE);

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur lors de la création de la ronde : " + ex.getMessage(), 6000,
                    Notification.Position.MIDDLE);
        }
    }
}
