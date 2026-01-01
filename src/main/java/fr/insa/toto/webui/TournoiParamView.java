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

/**
 *
 * @author ThinkPad
 */

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Tournoi;

import java.sql.Connection;
import java.sql.SQLException;

@Route(value = "tournoi") // ajoute layout si tu as un MainLayout
@PageTitle("Paramètres du tournoi")
public class TournoiParamView extends VerticalLayout {

    private final Connection con;

    private final TextField nom = new TextField("Nom du tournoi");
    private final IntegerField nbTerrains = new IntegerField("Nombre de terrains");
    private final IntegerField nbJoueursParEquipe = new IntegerField("Joueurs par équipe");

    private final Button enregistrer = new Button("Enregistrer");
    private final Button recharger = new Button("Recharger");

    private Tournoi tournoi;

    public TournoiParamView() throws SQLException {
        this.con = ConnectionSimpleSGBD.defaultCon();

        setMaxWidth("900px");
        setWidthFull();

        nbTerrains.setMin(1);
        nbJoueursParEquipe.setMin(1);

        add(
                new H2("Paramètres du tournoi"),
                nom, nbTerrains, nbJoueursParEquipe,
                new HorizontalLayout(enregistrer, recharger)
        );

        recharger.addClickListener(e -> {
            try { load(); } catch (SQLException ex) { throw new RuntimeException(ex); }
        });

        enregistrer.addClickListener(e -> {
            try { save(); } catch (SQLException ex) { throw new RuntimeException(ex); }
        });

        load();
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

