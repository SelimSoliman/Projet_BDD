package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// =====================================================
// VUE : Classement Global (Extension 2)
// =====================================================

@Route(value = "classement-global", layout = MainLayout.class)
@PageTitle("Classement Global")
public class ClassementGlobalView extends VerticalLayout {

    public ClassementGlobalView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Classement Global de tous les tournois"));

        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti.ClassementGlobalInfo> classement = 
                TournoiMulti.getClassementGlobal(con);

            if (classement.isEmpty()) {
                add(new Paragraph("Aucune donnée de classement disponible."));
                return;
            }

            Grid<TournoiMulti.ClassementGlobalInfo> grid = 
                new Grid<>(TournoiMulti.ClassementGlobalInfo.class, false);
            
            grid.addColumn(c -> classement.indexOf(c) + 1).setHeader("Rang");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getSurnom).setHeader("Joueur");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNom).setHeader("Nom");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbTournois)
                .setHeader("Tournois");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbMatchs)
                .setHeader("Matchs");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getNbVictoires)
                .setHeader("Victoires");
            grid.addColumn(TournoiMulti.ClassementGlobalInfo::getScoreTotal)
                .setHeader("Score Total");
            grid.addColumn(c -> String.format("%.1f%%", c.getTauxVictoire()))
                .setHeader("Taux victoire");

            grid.setItems(classement);
            add(grid);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}

// =====================================================
// VUE : Gestion des terrains avec plan (Extension 3)
// =====================================================

