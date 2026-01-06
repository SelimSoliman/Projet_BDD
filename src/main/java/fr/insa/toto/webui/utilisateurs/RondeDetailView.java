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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "rondes/:id",  layout=MainLayout.class)
@PageTitle("Détail ronde")
public class RondeDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final Paragraph header = new Paragraph();
    private final Grid<LigneMatchRonde> grid = new Grid<>(LigneMatchRonde.class, false);

    public static class LigneMatchRonde {
        public int matchId;
        public String statut;
        public Integer score1;
        public Integer score2;

        public LigneMatchRonde(int matchId, String statut, Integer score1, Integer score2) {
            this.matchId = matchId;
            this.statut = statut;
            this.score1 = score1;
            this.score2 = score2;
        }
    }

    public RondeDetailView() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Détail ronde"));
        add(header);

        grid.addComponentColumn(l ->
                        new RouterLink(
                                "Match " + l.matchId,
                                MatchDetailView.class,
                                new RouteParameters("id", String.valueOf(l.matchId))
                        )
                )
                .setHeader("Match").setAutoWidth(true);

        grid.addColumn(l -> l.statut)
                .setHeader("Statut").setAutoWidth(true);

        grid.addColumn(l -> (l.score1 == null ? "?" : l.score1) + " - " + (l.score2 == null ? "?" : l.score2))
                .setHeader("Score").setAutoWidth(true);

        add(grid);

        add(new RouterLink("Retour classement", ClassementView.class));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String idStr = event.getRouteParameters().get("id").orElse(null);
        if (idStr == null) return;

        int rondeId;
        try {
            rondeId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return;
        }

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {

            Integer num = getNumeroRonde(con, rondeId);
            header.setText(num == null ? ("Ronde id=" + rondeId) : ("Ronde " + num + " (id=" + rondeId + ")"));

            grid.setItems(matchsDeRondeRobuste(con, rondeId));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Integer getNumeroRonde(Connection con, int rondeId) throws SQLException {
        String sql = "SELECT numero FROM ronde WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rondeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return (Integer) rs.getObject("numero");
            }
        }
    }

    private List<LigneMatchRonde> matchsDeRondeRobuste(Connection con, int rondeId) throws SQLException {
        String[] matchTables = {"match", "matchs"};
        String[] rondeCols = {"ronde", "ronde_id"};

        SQLException last = null;
        for (String mt : matchTables) {
            for (String rc : rondeCols) {
                String sql = """
                    SELECT m.id AS match_id,
                           m.statut AS statut,
                           e1.score AS s1,
                           e2.score AS s2
                    FROM %s m
                    LEFT JOIN equipe e1 ON e1.id_match = m.id AND e1.numero = 1
                    LEFT JOIN equipe e2 ON e2.id_match = m.id AND e2.numero = 2
                    WHERE m.%s = ?
                    ORDER BY m.id
                    """.formatted(mt, rc);

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, rondeId);

                    List<LigneMatchRonde> res = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            res.add(new LigneMatchRonde(
                                    rs.getInt("match_id"),
                                    rs.getString("statut"),
                                    (Integer) rs.getObject("s1"),
                                    (Integer) rs.getObject("s2")
                            ));
                        }
                    }
                    return res;

                } catch (SQLException e) {
                    last = e;
                }
            }
        }
        if (last != null) throw last;
        return List.of();
    }
}
