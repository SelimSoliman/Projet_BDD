package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class Match extends ClasseMiroir {

    public enum Statut { EN_COURS, CLOS }

    private Ronde ronde;        // peut être null si "objet léger" pour affichage
    private Terrain terrain;    // peut être null si non chargé
    private Equipe equipe1;
    private Equipe equipe2;
    private Statut statut;

    // Nouveau match en mémoire
    public Match(Ronde ronde, Terrain terrain) {
        super();
        if (ronde == null) throw new IllegalArgumentException("ronde null");
        this.ronde = ronde;
        this.terrain = terrain;
        this.equipe1 = new Equipe(this, 1);
        this.equipe2 = new Equipe(this, 2);
        this.statut = Statut.EN_COURS;
    }

    // Match "depuis BD" (ronde/terrain peuvent être null si tu ne les charges pas)
    public Match(int id, Ronde ronde, Terrain terrain, int scoreE1, int scoreE2, Statut statut) {
        super(id);
        this.ronde = ronde;
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

    // ------------------------------------------------------------
    // CONSOLE-LIKE : matchs en cours de LA DERNIERE RONDE DU TOURNOI
    // (dernière ronde = numero max du tournoi unique)
    // ------------------------------------------------------------
    public static List<Match> matchsEnCoursDeLaDerniereRonde(Connection con) throws SQLException {
        List<Match> res = new ArrayList<>();

        Tournoi t = Tournoi.getUnique(con);
        if (t == null) return res;

        int rondeId = -1;
        String sqlDerniereRonde =
                "SELECT id " +
                "FROM ronde " +
                "WHERE id_tournoi = ? " +
                "ORDER BY numero DESC " +
                "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sqlDerniereRonde)) {
            ps.setInt(1, t.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) rondeId = rs.getInt("id");
            }
        }
        if (rondeId == -1) return res;

        String sqlMatchs =
                "SELECT id, terrain_id, score_e1, score_e2, statut " +
                "FROM matchs " +
                "WHERE ronde_id = ? AND statut = ? " +
                "ORDER BY id";

        try (PreparedStatement ps = con.prepareStatement(sqlMatchs)) {
            ps.setInt(1, rondeId);
            ps.setString(2, Statut.EN_COURS.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new Match(
                            rs.getInt("id"),
                            null, // ronde non chargée (pas besoin pour l'affichage)
                            null, // terrain non chargé
                            rs.getInt("score_e1"),
                            rs.getInt("score_e2"),
                            Statut.valueOf(rs.getString("statut"))
                    ));
                }
            }
        }

        return res;
    }

    // ------------------------------------------------------------
    // INSERT
    // ------------------------------------------------------------
    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql =
                "INSERT INTO matchs (ronde_id, terrain_id, score_e1, score_e2, statut) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, ronde.getId());

        if (terrain != null) ps.setInt(2, terrain.getId());
        else ps.setNull(2, Types.INTEGER);

        ps.setInt(3, getScoreEquipe1());
        ps.setInt(4, getScoreEquipe2());
        ps.setString(5, statut.name());

        ps.executeUpdate();
        return ps;
    }

    // ------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------
    public void updateInDB(Connection con) throws SQLException {
        String sql =
                "UPDATE matchs " +
                "SET terrain_id = ?, score_e1 = ?, score_e2 = ?, statut = ? " +
                "WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (terrain != null) ps.setInt(1, terrain.getId());
            else ps.setNull(1, Types.INTEGER);

            ps.setInt(2, getScoreEquipe1());
            ps.setInt(3, getScoreEquipe2());
            ps.setString(4, statut.name());
            ps.setInt(5, getId());
            ps.executeUpdate();
        }
      }

    // ------------------------------------------------------------
    // Clôture + fermeture auto de ronde si plus de matchs ouverts
    // ------------------------------------------------------------
    public static boolean validerEtCloturerMatch(
        Connection con,
        int matchId,
        int score1,
        int score2
) throws SQLException {

    boolean oldAutoCommit = con.getAutoCommit();
    con.setAutoCommit(false);

    boolean rondeFermee = false;

    try {
        // 1) Clôturer le match
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE matchs SET score_e1 = ?, score_e2 = ?, statut = ? " +
                "WHERE id = ? AND statut <> ?")) {

            ps.setInt(1, score1);
            ps.setInt(2, score2);
            ps.setString(3, Statut.CLOS.name());
            ps.setInt(4, matchId);
            ps.setString(5, Statut.CLOS.name());

            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("Match introuvable ou déjà clos");
            }
        }

        // 2) Récupérer la ronde du match
        int rondeId;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT ronde_id FROM matchs WHERE id = ?")) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Ronde introuvable");
                rondeId = rs.getInt(1);
            }
        }

        // 3) Vérifier s'il reste des matchs EN_COURS
        int nbOuverts;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM matchs WHERE ronde_id = ? AND statut = ?")) {
            ps.setInt(1, rondeId);
            ps.setString(2, Statut.EN_COURS.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                nbOuverts = rs.getInt(1);
            }
        }

        // 4) Fermer la ronde si plus aucun match ouvert
        if (nbOuverts == 0) {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE ronde SET close = 1 WHERE id = ? AND close = 0")) {
                ps.setInt(1, rondeId);
                ps.executeUpdate();
            }
            rondeFermee = true;
        }

        con.commit();
        return rondeFermee;

    } catch (SQLException e) {
        con.rollback();
        throw e;
    } finally {
        con.setAutoCommit(oldAutoCommit);
    }
}


    // ------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------
    public static int findRondeIdDuMatch(Connection con, int matchId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT ronde_id FROM matchs WHERE id = ?")) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return -1;
            }
        }
    }

    public static List<Match> matchsDeRonde(Connection con, int rondeId) throws SQLException {
        List<Match> res = new ArrayList<>();
        String sql =
                "SELECT id, terrain_id, score_e1, score_e2, statut " +
                "FROM matchs " +
                "WHERE ronde_id = ? " +
                "ORDER BY id";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rondeId);
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

    public static void supprimer(Connection con, int idMatch) throws SQLException {
        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM match_joueur WHERE id_match = ?")) {
                ps.setInt(1, idMatch);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM equipe WHERE id_match = ?")) {
                ps.setInt(1, idMatch);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM matchs WHERE id = ?")) {
                ps.setInt(1, idMatch);
                ps.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    public void saveEquipesEtJoueurs(Connection con) throws SQLException {
        // table equipe
        equipe1.saveInDB(con);
        equipe2.saveInDB(con);

        // table match_joueur
        equipe1.saveJoueursDansEquipe(con);
        equipe2.saveJoueursDansEquipe(con);
    }
    public static Match creerMatchEtSauver(
        Connection con,
        Ronde ronde,
        Terrain terrain,
        List<Joueur> equipe1,
        List<Joueur> equipe2
) throws SQLException {

    if (ronde == null) throw new IllegalArgumentException("ronde null");
    if (terrain == null) throw new IllegalArgumentException("terrain null");
    if (equipe1 == null || equipe2 == null) throw new IllegalArgumentException("equipes null");
    if (equipe1.isEmpty() || equipe2.isEmpty()) throw new IllegalArgumentException("equipes vides");

    Match m = new Match(ronde, terrain);

    // 1) INSERT matchs (donne un id)
    m.saveInDB(con);

    // 2) remplir équipes en mémoire
    for (Joueur j : equipe1) m.getEquipe1().ajouterJoueur(j);
    for (Joueur j : equipe2) m.getEquipe2().ajouterJoueur(j);

    // 3) INSERT equipe + match_joueur
    m.saveEquipesEtJoueurs(con);

    // 4) (optionnel) rattacher en mémoire
    // ronde.ajouterMatch(m);

    return m;
}
public int getIdRonde() { 
    return (ronde != null) ? ronde.getId() : -1; 
}

public int getIdEquipe1() {
    return (equipe1 != null) ? equipe1.getId() : -1;
}

public int getIdEquipe2() {
    return (equipe2 != null) ? equipe2.getId() : -1;
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