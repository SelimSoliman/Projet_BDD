/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route(value = "matchs/new", layout = MainLayout.class)
@PageTitle("Ajouter un match")
public class MatchCreateView extends VerticalLayout {

    private final Span infoRonde = new Span();

    private final ComboBox<Terrain> cbTerrain = new ComboBox<>("Terrain");
    private final ComboBox<Joueur> e1j1 = new ComboBox<>("Équipe 1 - Joueur 1");
    private final ComboBox<Joueur> e1j2 = new ComboBox<>("Équipe 1 - Joueur 2");
    private final ComboBox<Joueur> e2j1 = new ComboBox<>("Équipe 2 - Joueur 1");
    private final ComboBox<Joueur> e2j2 = new ComboBox<>("Équipe 2 - Joueur 2");

    private final Button creer = new Button("Créer le match (en base)");

    // Chargé depuis la BD
    private Tournoi tournoi;
    private Ronde rondeOuverte;

    public MatchCreateView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Ajouter un match"));

        if (!SessionInfo.adminConnected()) {
            add(new Span("Accès refusé : administrateur uniquement."));
            return;
        }

        cbTerrain.setItemLabelGenerator(t -> t.getId() + " - " + t.getNom());

        // labels joueurs (adapte si ton getter s'appelle autrement)
        e1j1.setItemLabelGenerator(j -> j.getId() + " - " + j.getSurnom());
        e1j2.setItemLabelGenerator(j -> j.getId() + " - " + j.getSurnom());
        e2j1.setItemLabelGenerator(j -> j.getId() + " - " + j.getSurnom());
        e2j2.setItemLabelGenerator(j -> j.getId() + " - " + j.getSurnom());

        add(infoRonde, cbTerrain, e1j1, e1j2, e2j1, e2j2, creer);

        chargerDonnees();

        creer.addClickListener(e -> creerMatch());
    }

    private void chargerDonnees() {
        try (Connection con = ConnectionPool.getConnection()) {

            tournoi = Tournoi.getTournoiUnique(con);
            if (tournoi == null) {
                infoRonde.setText("Aucun tournoi en base. Crée d’abord le tournoi.");
                disableForm();
                return;
            }

            // On prend la dernière ronde, et on exige qu’elle soit ouverte
            rondeOuverte = Ronde.findDerniereRonde(con, tournoi);
            if (rondeOuverte == null || rondeOuverte.isClose()) {
                infoRonde.setText("Aucune ronde ouverte. Crée une ronde avant d’ajouter des matchs.");
                disableForm();
                return;
            }

            infoRonde.setText("Ronde en cours : n°" + rondeOuverte.getNumero() + " (id=" + rondeOuverte.getId() + ")");

            // Terrains dispo
            List<Terrain> terrains = Terrain.tousLesTerrains(con).stream()
                    .filter(Terrain::estDisponible)
                    .toList();
            cbTerrain.setItems(terrains);

            // Joueurs
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            e1j1.setItems(joueurs);
            e1j2.setItems(joueurs);
            e2j1.setItems(joueurs);
            e2j2.setItems(joueurs);

            enableForm();

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur chargement : " + ex.getMessage(), 6000, Notification.Position.MIDDLE);
            disableForm();
        }
    }

    private void creerMatch() {
        if (rondeOuverte == null) {
            Notification.show("Aucune ronde ouverte.", 4000, Notification.Position.MIDDLE);
            return;
        }

        Terrain terrain = cbTerrain.getValue();
        if (terrain == null) {
            Notification.show("Choisis un terrain.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Joueur j11 = e1j1.getValue();
        Joueur j12 = e1j2.getValue();
        Joueur j21 = e2j1.getValue();
        Joueur j22 = e2j2.getValue();

        if (j11 == null || j12 == null || j21 == null || j22 == null) {
            Notification.show("Choisis 4 joueurs (2 par équipe).", 3500, Notification.Position.MIDDLE);
            return;
        }

        // Vérif : tous différents
        Set<Integer> ids = new HashSet<>();
        ids.add(j11.getId());
        ids.add(j12.getId());
        ids.add(j21.getId());
        ids.add(j22.getId());
        if (ids.size() != 4) {
            Notification.show("Un joueur est sélectionné plusieurs fois. Il faut 4 joueurs distincts.", 5000, Notification.Position.MIDDLE);
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1) créer match + insert matchs
                Match m = new Match(rondeOuverte, terrain);
                m.saveInDB(con); // => récupère id

                // 2) remplir équipes en mémoire
                m.getEquipe1().ajouterJoueur(j11);
                m.getEquipe1().ajouterJoueur(j12);
                m.getEquipe2().ajouterJoueur(j21);
                m.getEquipe2().ajouterJoueur(j22);

                // 3) insert equipe + match_joueur
                m.saveEquipesEtJoueurs(con);

                con.commit();

                Notification.show("Match créé en base ✅ (id=" + m.getId() + ")", 4500, Notification.Position.MIDDLE);

                // reset champs
                cbTerrain.clear();
                e1j1.clear(); e1j2.clear(); e2j1.clear(); e2j2.clear();

                // optionnel : recharger pour mettre à jour terrains dispo, etc.
                chargerDonnees();

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur création match : " + ex.getMessage(), 6000, Notification.Position.MIDDLE);
        }
    }

    private void disableForm() {
        cbTerrain.setEnabled(false);
        e1j1.setEnabled(false);
        e1j2.setEnabled(false);
        e2j1.setEnabled(false);
        e2j2.setEnabled(false);
        creer.setEnabled(false);
    }

    private void enableForm() {
        cbTerrain.setEnabled(true);
        e1j1.setEnabled(true);
        e1j2.setEnabled(true);
        e2j1.setEnabled(true);
        e2j2.setEnabled(true);
        creer.setEnabled(true);
    }
}
