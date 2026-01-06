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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "matchs/resultat", layout = MainLayout.class)
@PageTitle("Saisir résultat")
public class MatchResultView extends VerticalLayout {

    private final ComboBox<Integer> cbMatchId = new ComboBox<>("Match (id)");
    private final IntegerField score1 = new IntegerField("Score équipe 1");
    private final IntegerField score2 = new IntegerField("Score équipe 2");
    private final Button valider = new Button("Valider et clôturer");

    public MatchResultView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Saisir le résultat d’un match"));

        if (!SessionInfo.adminConnected()) {
            add("Accès refusé : administrateur uniquement.");
            return;
        }

        score1.setMin(0);
        score2.setMin(0);
        score1.setValue(0);
        score2.setValue(0);

        add(cbMatchId, score1, score2, valider);

        chargerMatchsEnCours();

        valider.addClickListener(e -> traiterCloture());
    }

    private void chargerMatchsEnCours() {
        try (Connection con = ConnectionPool.getConnection()) {

            // ⚠️ Utilise bien le nom exact de la méthode dans ta classe Match
            // Dans ton code, la méthode implémentée s'appelle :
            // matchsEnCoursDeDerniereRonde (sans "La")
            List<Match> matchs = Match.matchsEnCoursDeLaDerniereRonde(con);

            List<Integer> ids = matchs.stream().map(Match::getId).toList();
            cbMatchId.setItems(ids);
            cbMatchId.setValue(ids.isEmpty() ? null : ids.get(0));

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur chargement matchs", 4000, Notification.Position.MIDDLE);
        }
    }

    private void traiterCloture() {
        Integer matchId = cbMatchId.getValue();
        if (matchId == null) {
            Notification.show("Choisis un match.", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (score1.getValue() == null || score2.getValue() == null) {
            Notification.show("Renseigne les 2 scores.", 3000, Notification.Position.MIDDLE);
            return;
        }

        int s1 = score1.getValue();
        int s2 = score2.getValue();

try (Connection con = ConnectionPool.getConnection()) {

    boolean rondeFermee = Match.validerEtCloturerMatch(con, matchId, s1, s2);

    if (rondeFermee) {
        Notification.show(
            "Match clôturé ✅ + ronde clôturée automatiquement",
            4000,
            Notification.Position.MIDDLE
        );
    } else {
        Notification.show(
            "Match clôturé",
            3000,
            Notification.Position.MIDDLE
        );
    }
   

    chargerMatchsEnCours();

} catch (SQLException ex) {
    ex.printStackTrace();
    Notification.show(
        "Erreur : " + ex.getMessage(),
        5000,
        Notification.Position.MIDDLE
    );
}}}



       
  




  
