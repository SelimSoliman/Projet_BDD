package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "classement-global", layout = MainLayout.class)
@PageTitle("Classement Global")
public class ClassementGlobalView extends VerticalLayout {

    public ClassementGlobalView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Classement Global"));

        TournoiMulti tournoi = SessionInfo.getTournoiActif();
        if (tournoi == null) {
            add(new Paragraph("Aucun tournoi actif sélectionné."));
            add(new Paragraph("Sélectionne un tournoi dans 'Liste des tournois' puis réessaie."));
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {

            // ✅ appel NON static
            List<TournoiMulti.ClassementGlobalInfo> classement =
                    tournoi.getClassementGlobal(con);

            if (classement == null || classement.isEmpty()) {
                add(new Paragraph("Aucune donnée de classement disponible."));
                return;
            }

            Grid<TournoiMulti.ClassementGlobalInfo> grid =
                    new Grid<>(TournoiMulti.ClassementGlobalInfo.class, false);

            grid.addColumn(c -> classement.indexOf(c) + 1).setHeader("Rang");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getSurnom).setHeader("Joueur");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNom).setHeader("Nom");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbTournois).setHeader("Tournois");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbMatchs).setHeader("Matchs");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbVictoires).setHeader("Victoires");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getScoreTotal).setHeader("Score Total");
           

            grid.setItems(classement);
            add(grid);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}
