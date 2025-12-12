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


import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.toto.webui.MainLayout;
import java.util.List;

@Route(value = "utilisateurs/crationAdmin",layout= MainLayout.class)
@PageTitle("Teqball")
public class CreationAdmin extends FormLayout {

    private TextField surnom;
    private PasswordField password;
    private ComboBox<String> role;

    public CreationAdmin() {

        // Champs
        this.surnom = new TextField("Surnom");
        this.password = new PasswordField("Mot de passe");
        this.role = new ComboBox<>("Rôle");

        // Valeurs possibles du rôle
        this.role.setItems(List.of("utilisateur", "admin"));

        // Mise en forme
        this.setAutoResponsive(true);
        this.addFormRow(this.surnom,this.password);
        this.addFormRow(this.role);
        
           
        
    }
}

