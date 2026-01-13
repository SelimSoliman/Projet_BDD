package fr.insa.toto.webui.joueurs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension 1 : Interface spécifique pour les joueurs
 * Permet aux joueurs de visualiser facilement leurs propres données
 */
@Route(value = "joueur/mon-espace", layout = MainLayout.class)
@PageTitle("Mon Espace Joueur")
public class InterfaceJoueurView extends VerticalLayout {

    private Joueur joueurCourant;

    public InterfaceJoueurView() {
        setPadding(true);
        setSpacing(true);

        // ✅ Protection : connecté + joueur uniquement
        if (!SessionInfo.connected()) {
            UI.getCurrent().navigate("login");
            return;
        }
        if (!SessionInfo.playerConnected()) {
            UI.getCurrent().navigate("");
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {

            Integer idJoueur = SessionInfo.getIdJoueurConnecte();

            if (idJoueur == null) {
                add(new H2("Mon Espace Joueur"));
                add(new Paragraph("Votre compte joueur n'est pas lié à un profil Joueur (id_joueur manquant)."));
                return;
            }

            joueurCourant = findJoueurById(con, idJoueur);

            if (joueurCourant == null) {
                add(new H2("Mon Espace Joueur"));
                add(new Paragraph("Profil joueur introuvable pour id_joueur = " + idJoueur));
                return;
            }

            add(new H2("Mon Espace Joueur"));
            add(new H3("Informations personnelles"));
            afficherInfosPersonnelles();

            add(new H3("Mes statistiques"));
            afficherStatistiques(con);

            add(new H3("Mes matchs"));
            afficherMesMatchs(con);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erreur de chargement : " + ex.getMessage());
        }
    }

    private void afficherInfosPersonnelles() {
        VerticalLayout infos = new VerticalLayout();
        infos.add(new Paragraph("Surnom : " + joueurCourant.getSurnom()));
        infos.add(new Paragraph("Nom : " + joueurCourant.getNom()));
        infos.add(new Paragraph("Prénom : " + joueurCourant.getPrenom()));
        infos.add(new Paragraph("Catégorie : " + joueurCourant.getCategorie()));
        infos.add(new Paragraph("Taille : " + joueurCourant.getTaillecm() + " cm"));
        add(infos);
    }

    private void afficherStatistiques(Connection con) throws SQLException {
        Tournoi tournoi = Tournoi.getTournoiUnique(con);
        if (tournoi == null) {
            add(new Paragraph("Aucun tournoi en cours."));
            return;
        }

        int scoreTotal = calculerScoreTotal(con);
        int nbMatchsJoues = compterMatchsJoues(con);
        int victoires = compterVictoires(con);

        VerticalLayout stats = new VerticalLayout();
        stats.add(new Paragraph("Score total : " + scoreTotal));
        stats.add(new Paragraph("Matchs joués : " + nbMatchsJoues));
        stats.add(new Paragraph("Victoires : " + victoires));
        if (nbMatchsJoues > 0) {
            double tauxVictoire = (victoires * 100.0) / nbMatchsJoues;
            stats.add(new Paragraph(String.format("Taux de victoire : %.1f%%", tauxVictoire)));
        }
        add(stats);
    }

    private void afficherMesMatchs(Connection con) throws SQLException {
        List<MatchInfo> matchs = recupererMesMatchs(con);

        if (matchs.isEmpty()) {
            add(new Paragraph("Vous n'avez pas encore participé à des matchs."));
            return;
        }

        Grid<MatchInfo> grid = new Grid<>(MatchInfo.class, false);
        grid.addColumn(MatchInfo::getRonde).setHeader("Ronde");
        grid.addColumn(MatchInfo::getStatut).setHeader("Statut");
        grid.addColumn(MatchInfo::getScoreEquipe).setHeader("Score équipe");
        grid.addColumn(MatchInfo::getScoreAdverse).setHeader("Score adverse");
        grid.addColumn(m -> m.isVictoire() ? "Victoire" : "Défaite").setHeader("Résultat");
        grid.addColumn(MatchInfo::getPartenaire).setHeader("Partenaire");

        grid.setItems(matchs);
        add(grid);
    }

    // ✅ Nouveau : récupérer joueur par id
    private Joueur findJoueurById(Connection con, int id) throws SQLException {
        String sql = "SELECT * FROM joueur WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Joueur(
                            rs.getInt("id"),
                            rs.getString("surnom"),
                            rs.getString("categorie"),
                            rs.getInt("taillecm"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("sexe"),
                            rs.getDate("date_naissance") != null
                                    ? rs.getDate("date_naissance").toLocalDate()
                                    : null
                    );
                }
            }
        }
        return null;
    }

    private int calculerScoreTotal(Connection con) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(e.score), 0) AS total
            FROM match_joueur mj
            JOIN equipe e ON e.id_match = mj.id_match AND e.numero = mj.numero_equipe
            WHERE mj.id_joueur = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, joueurCourant.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("total");
            }
        }
    }

    private int compterMatchsJoues(Connection con) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT id_match) FROM match_joueur WHERE id_joueur = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, joueurCourant.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int compterVictoires(Connection con) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT m.id, 
                       e1.score as mon_score,
                       e2.score as score_adverse
                FROM matchs m
                JOIN match_joueur mj ON mj.id_match = m.id
                JOIN equipe e1 ON e1.id_match = m.id AND e1.numero = mj.numero_equipe
                JOIN equipe e2 ON e2.id_match = m.id AND e2.numero != mj.numero_equipe
                WHERE mj.id_joueur = ? AND m.statut = 'CLOS'
            ) AS matchs_joues
            WHERE mon_score > score_adverse
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, joueurCourant.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<MatchInfo> recupererMesMatchs(Connection con) throws SQLException {
        List<MatchInfo> result = new ArrayList<>();
        String sql = """
            SELECT m.id, r.numero as ronde, m.statut,
                   e1.score as mon_score, e2.score as score_adverse,
                   j2.surnom as partenaire
            FROM matchs m
            JOIN ronde r ON r.id = m.ronde_id
            JOIN match_joueur mj ON mj.id_match = m.id AND mj.id_joueur = ?
            JOIN equipe e1 ON e1.id_match = m.id AND e1.numero = mj.numero_equipe
            JOIN equipe e2 ON e2.id_match = m.id AND e2.numero != mj.numero_equipe
            LEFT JOIN match_joueur mj2 ON mj2.id_match = m.id 
                AND mj2.numero_equipe = mj.numero_equipe 
                AND mj2.id_joueur != ?
            LEFT JOIN joueur j2 ON j2.id = mj2.id_joueur
            ORDER BY r.numero DESC
            """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, joueurCourant.getId());
            ps.setInt(2, joueurCourant.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MatchInfo(
                            rs.getInt("ronde"),
                            rs.getString("statut"),
                            rs.getInt("mon_score"),
                            rs.getInt("score_adverse"),
                            rs.getString("partenaire")
                    ));
                }
            }
        }
        return result;
    }

    public static class MatchInfo {
        private int ronde;
        private String statut;
        private int scoreEquipe;
        private int scoreAdverse;
        private String partenaire;

        public MatchInfo(int ronde, String statut, int scoreEquipe, int scoreAdverse, String partenaire) {
            this.ronde = ronde;
            this.statut = statut;
            this.scoreEquipe = scoreEquipe;
            this.scoreAdverse = scoreAdverse;
            this.partenaire = partenaire;
        }

        public boolean isVictoire() {
            return scoreEquipe > scoreAdverse;
        }

        public int getRonde() { return ronde; }
        public String getStatut() { return statut; }
        public int getScoreEquipe() { return scoreEquipe; }
        public int getScoreAdverse() { return scoreAdverse; }
        public String getPartenaire() { return partenaire != null ? partenaire : "Aucun"; }
    }
}
