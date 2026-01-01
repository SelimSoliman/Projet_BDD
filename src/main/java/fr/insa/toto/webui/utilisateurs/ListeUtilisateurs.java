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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.MainLayout;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author ThinkPad
 */
@Route(value = "utilisateurs/liste", layout = MainLayout.class)
@PageTitle("Teqball")
public class ListeUtilisateurs extends VerticalLayout  {
    public ListeUtilisateurs() {
        this.add(new H2("Liste de tous les utilisateurs"));

        Grid<Utilisateur> grid = new Grid<>(Utilisateur.class, false);
        grid.addColumn(Utilisateur::getSurnom).setHeader("surnom");
        grid.addColumn(Utilisateur::getRole).setHeader("roleID");
        grid.addColumn(u ->u.getRole() == 1 ? "admin":"utilisateur").setHeader("role");
       grid.addColumn(new ComponentRenderer<Icon, Utilisateur>(u -> {
    return u.getRole() == 1
            ? VaadinIcon.THUMBS_UP.create()
            : VaadinIcon.THUMBS_DOWN.create();
})).setHeader("admin?");

        try (Connection con = ConnectionPool.getConnection()){
        grid.setItems(Utilisateur.tousLesUtilisateur(con));
         } catch (SQLException ex){
             Notification.show("Problème :" +    ex.getLocalizedMessage());
        
             System.getLogger(ListeUtilisateurs.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
             
         }
        

        this.add(grid);
    }

    
    
}




    


    
