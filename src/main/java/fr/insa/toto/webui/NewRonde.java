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
                Notification.show("Impossible : la ronde " + derniere.getNumero() + " est encore en cours.",
                        4000, Notification.Position.MIDDLE);
                return;
            }

            // 3) charger joueurs + terrains depuis la base (AVANT de créer la ronde)
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);

            int tailleEquipe = tournoi.getNbJoueursParEquipe(); // chez toi = 2
            int minJoueursPour1Match = 2 * tailleEquipe;

            if (joueurs.size() < minJoueursPour1Match) {
                Notification.show(
                        "Pas assez de joueurs : il faut au minimum " + minJoueursPour1Match
                                + " (actuellement " + joueurs.size() + ").",
                        5000, Notification.Position.MIDDLE
                );
                return;
            }

            List<Terrain> terrainsDispo = Terrain.tousLesTerrains(con).stream()
                    .filter(Terrain::estDisponible)
                    .limit(tournoi.getNbTerrains()) // on ne prend que nbTerrains
                    .collect(Collectors.toList());

            if (terrainsDispo.isEmpty()) {
                Notification.show("Aucun terrain disponible : impossible de générer des matchs.",
                        5000, Notification.Position.MIDDLE);
                return;
            }

            // 4) créer ronde
            int numero = (derniere == null) ? 1 : derniere.getNumero() + 1;
            Ronde nouvelle = new Ronde(tournoi, numero);
            nouvelle.saveInDB(con);

            // 5) remplir les listes en mémoire du tournoi (pour creerMatchsPourRonde)
            tournoi.clearJoueurs();
            joueurs.forEach(tournoi::ajouterJoueur);

            tournoi.clearTerrains();
            terrainsDispo.forEach(tournoi::ajouterTerrain);

            // 6) calcul : combien de matchs vont réellement être créés + joueurs "sur le banc"
            int joueursParMatch = 2 * tailleEquipe;
            int nbMatchsPossibleParJoueurs = joueurs.size() / joueursParMatch;
            int nbMatchs = Math.min(nbMatchsPossibleParJoueurs, terrainsDispo.size());
            int joueursUtilises = nbMatchs * joueursParMatch;
            int joueursRestants = joueurs.size() - joueursUtilises;

            // 7) générer matchs
            tournoi.creerMatchsPourRonde(nouvelle, tailleEquipe, con);
int nb = 0;

            // 8) message final (avec cas "pas assez pour tout le monde")
            if (joueursRestants > 0) {
                Notification.show(
                        "Ronde " + numero + " créée ✅ (" + nbMatchs + " match(s)). "
                                + joueursRestants + " joueur(s) n'ont pas pu jouer (pas assez de terrains ou joueurs restants).",
                        6000, Notification.Position.MIDDLE
                );
            } else {
                Notification.show("Ronde " + numero + " créée + matchs générés ✅",
                        4000, Notification.Position.MIDDLE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur lors de la création de la ronde : " + ex.getMessage(),
                    6000, Notification.Position.MIDDLE);
        }
    }
}
