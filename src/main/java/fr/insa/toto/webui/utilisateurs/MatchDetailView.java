package fr.insa.toto.webui.utilisateurs;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.joueurs.ListeJoueursView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "matchs/:id", layout=MainLayout.class)
@PageTitle("Détail match")
public class MatchDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final Paragraph header = new Paragraph();
    private final VerticalLayout equipe1 = new VerticalLayout();
    private final VerticalLayout equipe2 = new VerticalLayout();
    private final HorizontalLayout nav = new HorizontalLayout();

    public MatchDetailView() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Détail match"));
        add(header);

        equipe1.setPadding(false);
        equipe2.setPadding(false);

        add(new H2("Equipe 1"), equipe1);
        add(new H2("Equipe 2"), equipe2);
        add(nav);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String idStr = event.getRouteParameters().get("id").orElse(null);
        if (idStr == null) return;

        int matchId;
        try { matchId = Integer.parseInt(idStr); }
        catch (NumberFormatException e) { return; }

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            MatchInfo info = getMatchInfoRobuste(con, matchId);
            if (info == null) {
                header.setText("Match introuvable (id=" + matchId + ")");
                equipe1.removeAll();
                equipe2.removeAll();
                nav.removeAll();
                return;
            }

            header.setText("Match " + matchId
                    + " | statut=" + (info.statut == null ? "?" : info.statut)
                    + " | score=" + val(info.score1) + "-" + val(info.score2));

            equipe1.removeAll();
            equipe2.removeAll();

            equipe1.add(new Paragraph("Score équipe 1 : " + val(info.score1)));
            for (JoueurLight j : joueursEquipe(con, matchId, 1)) {
                equipe1.add(new RouterLink(
                        j.surnom + " (" + j.nom + " " + j.prenom + ")",
                        JoueurDetailView.class,
                        new RouteParameters("id", String.valueOf(j.id))
                ));
            }

            equipe2.add(new Paragraph("Score équipe 2 : " + val(info.score2)));
            for (JoueurLight j : joueursEquipe(con, matchId, 2)) {
                equipe2.add(new RouterLink(
                        j.surnom + " (" + j.nom + " " + j.prenom + ")",
                        JoueurDetailView.class,
                        new RouteParameters("id", String.valueOf(j.id))
                ));
            }

            nav.removeAll();
            if (info.rondeId != null) {
                nav.add(new RouterLink(
                        "Voir la ronde",
                        RondeDetailView.class,
                        new RouteParameters("id", String.valueOf(info.rondeId))
                ));
            }
            nav.add(new RouterLink("Retour joueurs", ListeJoueursView.class));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String val(Integer x) {
        return x == null ? "?" : x.toString();
    }

    private static class MatchInfo {
        Integer rondeId;
        Integer score1;
        Integer score2;
        String statut;
        MatchInfo(Integer rondeId, Integer score1, Integer score2, String statut) {
            this.rondeId = rondeId;
            this.score1 = score1;
            this.score2 = score2;
            this.statut = statut;
        }
    }

    private static class JoueurLight {
        int id;
        String surnom;
        String nom;
        String prenom;
        JoueurLight(int id, String surnom, String nom, String prenom) {
            this.id = id;
            this.surnom = surnom;
            this.nom = nom;
            this.prenom = prenom;
        }
    }

    private MatchInfo getMatchInfoRobuste(Connection con, int matchId) throws SQLException {
        String[] matchTables = {"match", "matchs"};
        String[] rondeCols = {"ronde", "ronde_id"};

        SQLException last = null;
        for (String mt : matchTables) {
            for (String rc : rondeCols) {
                String sql = """
                    SELECT m.%s AS ronde_id,
                           m.statut AS statut,
                           e1.score AS s1,
                           e2.score AS s2
                    FROM %s m
                    LEFT JOIN equipe e1 ON e1.id_match = m.id AND e1.numero = 1
                    LEFT JOIN equipe e2 ON e2.id_match = m.id AND e2.numero = 2
                    WHERE m.id = ?
                    """.formatted(rc, mt);

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, matchId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return null;
                        return new MatchInfo(
                                (Integer) rs.getObject("ronde_id"),
                                (Integer) rs.getObject("s1"),
                                (Integer) rs.getObject("s2"),
                                rs.getString("statut")
                        );
                    }
                } catch (SQLException e) {
                    last = e;
                }
            }
        }
        if (last != null) throw last;
        return null;
    }

    private List<JoueurLight> joueursEquipe(Connection con, int matchId, int numeroEquipe) throws SQLException {
        String sql = """
            SELECT j.id, j.surnom, j.nom, j.prenom
            FROM match_joueur mj
            JOIN joueur j ON j.id = mj.id_joueur
            WHERE mj.id_match = ? AND mj.numero_equipe = ?
            ORDER BY j.surnom
            """;

        List<JoueurLight> res = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, numeroEquipe);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new JoueurLight(
                            rs.getInt("id"),
                            rs.getString("surnom"),
                            rs.getString("nom"),
                            rs.getString("prenom")
                    ));
                }
            }
        }
        return res;
    }
}