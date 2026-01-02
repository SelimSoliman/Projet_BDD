package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Match extends ClasseMiroir {

    public static List<Match> matchsEnCoursDeLaDerniereRonde(Connection con) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public enum Statut { EN_COURS, CLOS }

    private Ronde ronde;        // peut être null si "objet léger" pour affichage
    private Terrain terrain;    // peut être null
    private Equipe equipe1;
    private Equipe equipe2;
    private Statut statut = Statut.EN_COURS;

    // Nouveau match en mémoire
    public Match(Ronde ronde, Terrain terrain) {
        super();
        if (ronde == null) throw new IllegalArgumentException("ronde null");
        this.ronde = ronde;
        this.terrain = terrain;
        this.equipe1 = new Equipe(this, 1);
        this.equipe2 = new Equipe(this, 2);
    }

    // Match "depuis BD" (ronde peut être null si on ne l’a pas chargée)
    public Match(int id, Ronde ronde, Terrain terrain, int scoreE1, int scoreE2, Statut statut) {
        super(id);
        this.ronde = ronde; // ✅ accepte null
        this.terrain = terrain;
        this.equipe1 = new Equipe(this, 1);
        this.equipe2 = new Equipe(this, 2);
        this.equipe1.setScore(scoreE1);
        this.equipe2.setScore(scoreE2);
        this.statut = (statut == null) ? Statut.EN_COURS : statut;
    }

    public Ronde getRonde() { return ronde; }
    public Terrain getTerrain() { return terrain; }
    public void setTerrain(Terrain terrain) { this.terrain = terrain; }

    public Equipe getEquipe1() { return equipe1; }
    public Equipe getEquipe2() { return equipe2; }

    public int getScoreEquipe1() { return equipe1.getScoreTotal(); }
    public int getScoreEquipe2() { return equipe2.getScoreTotal(); }

    public Statut getStatut() { return statut; }
    public boolean estClos() { return statut == Statut.CLOS; }

    public void setScoreEquipe1(int score) { equipe1.setScore(score); }
    public void setScoreEquipe2(int score) { equipe2.setScore(score); }

    public void definirScores(int score1, int score2) {
        if (estClos()) throw new IllegalStateException("Match déjà clos");
        if (score1 < 0 || score2 < 0) throw new IllegalArgumentException("Scores négatifs interdits");
        setScoreEquipe1(score1);
        setScoreEquipe2(score2);
        this.statut = Statut.CLOS;
    }

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO matchs (ronde_id, terrain_id, score_e1, score_e2, statut)
            VALUES (?, ?, ?, ?, ?)
            """;
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, ronde.getId());
        if (terrain != null) ps.setInt(2, terrain.getId());
        else ps.setNull(2, java.sql.Types.INTEGER);

        ps.setInt(3, getScoreEquipe1());
        ps.setInt(4, getScoreEquipe2());
        ps.setString(5, statut.name());

        ps.executeUpdate();
        return ps;
    }

    public void updateInDB(Connection con) throws SQLException {
        String sql = """
            UPDATE matchs
            SET terrain_id = ?, score_e1 = ?, score_e2 = ?, statut = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (terrain != null) ps.setInt(1, terrain.getId());
            else ps.setNull(1, java.sql.Types.INTEGER);

            ps.setInt(2, getScoreEquipe1());
            ps.setInt(3, getScoreEquipe2());
            ps.setString(4, statut.name());
            ps.setInt(5, getId());
            ps.executeUpdate();
        }
    }

    public static void cloturerMatch(Connection con, int matchId, int score1, int score2) throws SQLException {
        String sqlMatch = """
            UPDATE matchs
            SET score_e1 = ?, score_e2 = ?, statut = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sqlMatch)) {
            ps.setInt(1, score1);
            ps.setInt(2, score2);
            ps.setString(3, Statut.CLOS.name());
            ps.setInt(4, matchId);
            ps.executeUpdate();
        }

        // si ta table equipe contient aussi les scores
        String sqlEquipe = "UPDATE equipe SET score = ? WHERE id_match = ? AND numero = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlEquipe)) {
            ps.setInt(1, score1);
            ps.setInt(2, matchId);
            ps.setInt(3, 1);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(sqlEquipe)) {
            ps.setInt(1, score2);
            ps.setInt(2, matchId);
            ps.setInt(3, 2);
            ps.executeUpdate();
        }
    }

    public static int findRondeIdDuMatch(Connection con, int matchId) throws SQLException {
        String sql = "SELECT ronde_id FROM matchs WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public static List<Integer> idsMatchsEnCours(Connection con) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM matchs WHERE statut = ? ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, Statut.EN_COURS.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    // ✅ UNE SEULE VERSION
    public static List<Match> matchsEnCoursDeDerniereRonde(Connection con) throws SQLException {
        List<Match> res = new ArrayList<>();

        String sqlDerniereRonde = """
            SELECT id
            FROM ronde
            ORDER BY numero DESC
            LIMIT 1
            """;

        int rondeId = -1;
        try (PreparedStatement ps = con.prepareStatement(sqlDerniereRonde);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) rondeId = rs.getInt(1);
        }
        if (rondeId == -1) return res;

        String sqlMatchs = """
            SELECT id, terrain_id, score_e1, score_e2, statut
            FROM matchs
            WHERE ronde_id = ? AND statut = ?
            """;

        try (PreparedStatement ps = con.prepareStatement(sqlMatchs)) {
            ps.setInt(1, rondeId);
            ps.setString(2, Statut.EN_COURS.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new Match(
                            rs.getInt("id"),
                            null,
                            null,
                            rs.getInt("score_e1"),
                            rs.getInt("score_e2"),
                            Statut.valueOf(rs.getString("statut"))
                    ));
                }
            }
        }
        return res;
    }

    public static void supprimer(Connection con, int id) throws SQLException {
        String sql = "DELETE FROM matchs WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public String toString() {
        String nomTerrain = (terrain == null) ? "aucun terrain" : terrain.getNom();
        return "Match " + getId() + " sur " + nomTerrain
                + " | E1=" + getScoreEquipe1()
                + " | E2=" + getScoreEquipe2()
                + " (" + statut + ")";
    }
}
