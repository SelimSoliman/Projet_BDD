package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
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
@Route(value = "tournois", layout = MainLayout.class)
@PageTitle("Liste des tournois")



public class ListeTournoisView extends VerticalLayout {

    private Grid<TournoiMulti> grid;

    public ListeTournoisView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Liste de tous les tournois"));

        grid = new Grid<>(TournoiMulti.class, false);
        grid.addColumn(TournoiMulti::getNom).setHeader("Nom");
        grid.addColumn(t -> t.getDateCreation().toLocalDate()).setHeader("Date création");
        grid.addColumn(t -> t.getDateDebut() != null 
            ? t.getDateDebut().toLocalDate() 
            : "Non démarré").setHeader("Date début");
        grid.addColumn(t -> t.getDateFin() != null 
            ? t.getDateFin().toLocalDate() 
            : "-").setHeader("Date fin");
        grid.addColumn(t -> t.getStatut().toString()).setHeader("Statut");
        grid.addColumn(TournoiMulti::getNbTerrains).setHeader("Nb terrains");

        add(grid);
        chargerTournois();
    }

    private void chargerTournois() {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti> tournois = TournoiMulti.tousLesTournois(con);
            grid.setItems(tournois);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur : " + ex.getMessage());
        }
    }
}