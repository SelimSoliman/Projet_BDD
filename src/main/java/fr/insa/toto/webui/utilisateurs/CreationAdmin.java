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

/**
 *
 * @author ThinkPad
 */


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.checkerframework.checker.units.qual.t;

@Route(value = "utilisateurs/creationAdmin", layout = MainLayout.class)
@PageTitle("Teqball")
public class CreationAdmin extends FormLayout {

    private TextField surnom;
    private PasswordField password;
    private ComboBox<String> role;
    private Button save;

    public CreationAdmin() {

        // ✅ PROTECTION : seuls les admins accèdent
        if (!SessionInfo.adminConnected()) {
            UI.getCurrent().navigate(""); // ou "login"
            return;
        }

        // Champs
        this.surnom = new TextField("Surnom");
        this.password = new PasswordField("Mot de passe");
        this.role = new ComboBox<>("Rôle");

        // Valeurs possibles du rôle
        this.role.setItems("utilisateur", "admin");
        this.role.setValue("utilisateur");

        this.save = new Button("save");
        this.save.addClickListener(t -> this.doSave());

        // Mise en forme / layout (à faire UNE FOIS ici, pas dans le click)
        this.setAutoResponsive(true);
        this.addFormRow(this.surnom, this.password);
        this.addFormRow(this.role);
        this.addFormRow(this.save);
    }

    public void doSave() {
        try (Connection con = ConnectionPool.getConnection()) {
            String surnom = this.surnom.getValue();
            String pass = this.password.getValue();

            int roleInt = 2;
            if ("admin".equals(this.role.getValue())) {
                roleInt = 1;
            }
            Utilisateur u = new Utilisateur(surnom, pass, roleInt);

            u.saveInDB(con);
            Notification.show("utilisateur " + surnom + " crée");

        } catch (SQLException ex) {
            Notification.show("Problème: " + ex.getLocalizedMessage());
        }
    }
}


