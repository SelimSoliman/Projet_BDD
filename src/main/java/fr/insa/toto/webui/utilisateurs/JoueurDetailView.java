package fr.insa.toto.webui.utilisateurs;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "joueurs/:id", layout=MainLayout.class)
@PageTitle("Détail joueur")
public class JoueurDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final Paragraph infos = new Paragraph();
    private final Grid<LigneMatch> gridMatchs = new Grid<>(LigneMatch.class, false);

    public static class LigneMatch {
        public int matchId;
        public Integer rondeId;
        public Integer rondeNumero;
        public Integer score1;
        public Integer score2;
        public String statut;

        public LigneMatch(int matchId, Integer rondeId, Integer rondeNumero, Integer score1, Integer score2, String statut) {
            this.matchId = matchId;
            this.rondeId = rondeId;
            this.rondeNumero = rondeNumero;
            this.score1 = score1;
            this.score2 = score2;
            this.statut = statut;
        }
    }

    public JoueurDetailView() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Détail joueur"));
        add(infos);

        gridMatchs.addComponentColumn(l ->
                        new RouterLink(
                                "Match " + l.matchId,
                                MatchDetailView.class,
                                new RouteParameters("id", String.valueOf(l.matchId))
                        )
                )
                .setHeader("Match").setAutoWidth(true);

        gridMatchs.addColumn(l -> l.statut)
                .setHeader("Statut").setAutoWidth(true);

        gridMatchs.addColumn(l -> (l.score1 == null ? "?" : l.score1) + " - " + (l.score2 == null ? "?" : l.score2))
                .setHeader("Score (E1-E2)").setAutoWidth(true);

        gridMatchs.addComponentColumn(l -> {
                    if (l.rondeId == null) return new Paragraph("-");
                    String txt = (l.rondeNumero == null) ? ("Ronde " + l.rondeId) : ("Ronde " + l.rondeNumero);
                    return new RouterLink(
                            txt,
                            RondeDetailView.class,
                            new RouteParameters("id", String.valueOf(l.rondeId))
                    );
                })
                .setHeader("Ronde").setAutoWidth(true);

        add(new H2("Matchs du joueur"), gridMatchs);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String idStr = event.getRouteParameters().get("id").orElse(null);
        if (idStr == null) return;

        int joueurId;
        try {
            joueurId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return;
        }

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {

            Joueur j = findJoueurById(con, joueurId);
            if (j == null) {
                infos.setText("Joueur introuvable (id=" + joueurId + ")");
                gridMatchs.setItems(List.of());
                return;
            }

            int score = 0;
            Tournoi t = Tournoi.getUnique(con);
            if (t != null) score = t.computeScore(j, con);

            infos.setText(
                    j.getSurnom() + " — " + j.getNom() + " " + j.getPrenom()
                            + " | cat=" + j.getCategorie()
                            + " | score=" + score
            );

            gridMatchs.setItems(matchsDuJoueurRobuste(con, joueurId));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Joueur findJoueurById(Connection con, int id) throws SQLException {
        for (Joueur j : Joueur.tousLesJoueurs(con)) {
            if (j.getId() == id) return j;
        }
        return null;
    }

    private List<LigneMatch> matchsDuJoueurRobuste(Connection con, int joueurId) throws SQLException {
        // variantes possibles selon ton schéma
        String[] matchTables = {"match", "matchs"};
        String[] rondeCols = {"ronde", "ronde_id"};

        List<LigneMatch> res = new ArrayList<>();
        SQLException last = null;

        for (String mt : matchTables) {
            for (String rc : rondeCols) {

                String sql = """
                    SELECT m.id AS match_id,
                           m.%s AS ronde_id,
                           r.numero AS ronde_num,
                           e1.score AS s1,
                           e2.score AS s2,
                           m.statut AS statut
                    FROM match_joueur mj
                    JOIN %s m ON m.id = mj.id_match
                    LEFT JOIN ronde r ON r.id = m.%s
                    LEFT JOIN equipe e1 ON e1.id_match = m.id AND e1.numero = 1
                    LEFT JOIN equipe e2 ON e2.id_match = m.id AND e2.numero = 2
                    WHERE mj.id_joueur = ?
                    ORDER BY m.id DESC
                    """.formatted(rc, mt, rc);

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, joueurId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            res.add(new LigneMatch(
                                    rs.getInt("match_id"),
                                    (Integer) rs.getObject("ronde_id"),
                                    (Integer) rs.getObject("ronde_num"),
                                    (Integer) rs.getObject("s1"),
                                    (Integer) rs.getObject("s2"),
                                    rs.getString("statut")
                            ));
                        }
                    }
                } catch (SQLException e) {
                    last = e;
                    res.clear();
                }

                if (!res.isEmpty()) return res;
            }
        }

        if (last != null) throw last;
        return res;
    }
}
