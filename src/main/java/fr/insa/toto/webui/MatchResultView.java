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

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
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
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            List<fr.insa.toto.model.Match> matchs = Match.matchsEnCoursDeLaDerniereRonde(con);
            List<Integer> ids = matchs.stream().map(Match::getId).toList();
            cbMatchId.setItems(ids);
            if (!ids.isEmpty()) cbMatchId.setValue(ids.get(0));
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur chargement matchs", 4000, Notification.Position.MIDDLE);
        }
    }

    private void traiterCloture() {
        Integer idMatch = cbMatchId.getValue();
        if (idMatch == null) {
            Notification.show("Choisis un match.", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (score1.getValue() == null || score2.getValue() == null) {
            Notification.show("Renseigne les 2 scores.", 3000, Notification.Position.MIDDLE);
            return;
        }

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            Match.cloturerMatch(con, idMatch, score1.getValue(), score2.getValue());

            int rondeId = Match.findRondeIdDuMatch(con, idMatch);

            boolean rondeFermee = (rondeId != -1) && Ronde.tryCloseRonde(con, rondeId);

            if (rondeFermee) {
                Notification.show("Match clôturé. ✅ Ronde clôturée automatiquement !", 4000, Notification.Position.MIDDLE);
            } else {
                Notification.show("Match clôturé. (La ronde continue)", 3500, Notification.Position.MIDDLE);
            }

            chargerMatchsEnCours();

        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erreur clôture match", 5000, Notification.Position.MIDDLE);
        }
    }
}

  
