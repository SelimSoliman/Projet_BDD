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
package fr.insa.toto.webui.utilisateurs;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "joueurs", layout=MainLayout.class)
@PageTitle("Joueurs")
public class JoueursView extends VerticalLayout {

    public JoueursView() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Joueurs"));

        Grid<Joueur> grid = new Grid<>(Joueur.class, false);
        grid.addColumn(Joueur::getId).setHeader("Id").setAutoWidth(true);
        grid.addComponentColumn(j ->
    new RouterLink(
        j.getSurnom(),
        JoueurDetailView.class,
        new RouteParameters("id", String.valueOf(j.getId()))
    )
).setHeader("Surnom").setAutoWidth(true);

        grid.addColumn(Joueur::getNom).setHeader("Nom").setAutoWidth(true);
        grid.addColumn(Joueur::getPrenom).setHeader("Prénom").setAutoWidth(true);
        grid.addColumn(Joueur::getCategorie).setHeader("Catégorie").setAutoWidth(true);

        add(grid);

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            grid.setItems(joueurs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

