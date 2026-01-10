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
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.UI;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.session.SessionInfo;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

@Route("login")
@PageTitle("Login")
public class LoginView extends VerticalLayout {

    public LoginView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        H2 title = new H2("Connexion");

        LoginForm form = new LoginForm();
        form.addLoginListener(e -> {
            String surnom = e.getUsername();
            String pass = e.getPassword();

            try (Connection con = ConnectionPool.getConnection()) {
               


                Optional<Utilisateur> trouve = Utilisateur.findBySurnomPass(con, surnom, pass);

                if (trouve.isEmpty()) {
                    form.setError(true);
                } else {
                   SessionInfo.setUserConnected(trouve.get());

UI.getCurrent().navigate(""); // accueil
                }

            } catch (SQLException ex) {
                Notification.show("Problème : " + ex.getLocalizedMessage());
            }
        });

        add(title, form);
    }
}

