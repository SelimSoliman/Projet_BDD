package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Match extends ClasseMiroir {

    public enum Statut {
        EN_COURS,
        CLOS
    }

    // --- attributs BD ---
    private Ronde ronde;        // ronde_id
    private Terrain terrain;    // terrain_id (peut être null)
    private Equipe equipe1;
    private Equipe equipe2;
    private Statut statut = Statut.EN_COURS;

    // --- constructeur pour nouveau match ---
    public Match(Ronde ronde, Terrain terrain) {
        super();          // id = -1
        this.ronde = ronde;
        this.terrain = terrain;
        this.equipe1 = new Equipe(this, 1);
        this.equipe2 = new Equipe(this, 2);
    }

    // --- constructeur depuis la BD (si besoin) ---
    public Match(int id, Ronde ronde, Terrain terrain,
                 int scoreE1, int scoreE2, Statut statut) {
        super(id);
        this.ronde = ronde;
        this.terrain = terrain;
        this.equipe1 = new Equipe(this, 1);
        this.equipe2 = new Equipe(this, 2);
        this.equipe1.ajouterScore(scoreE1);
        this.equipe2.ajouterScore(scoreE2);
        this.statut = statut;
    }

    // ---------------- GETTERS / SETTERS ----------------

    public Ronde getRonde() { return ronde; }

    public Terrain getTerrain() { return terrain; }
    public void setTerrain(Terrain terrain) { this.terrain = terrain; }

    public Equipe getEquipe1() { return equipe1; }

    public Equipe getEquipe2() { return equipe2; }

    public int getScoreEquipe1() { return equipe1.getScoreTotal(); }

    public int getScoreEquipe2() { return equipe2.getScoreTotal(); }

    public Statut getStatut() { return statut; }

    public boolean estClos() { return statut == Statut.CLOS; }

    // setters de scores pour le menu "modifier match"
    public void setScoreEquipe1(int score) {
    // on suppose ici que le score total d une equipe pour ce match
    // est simplement remplace par le nouveau score
    // si Equipe ne stocke qu un score cumule, on peut utiliser ajouterScore
    equipe1.ajouterScore(score);
}

public void setScoreEquipe2(int score) {
    equipe2.ajouterScore(score);
}


    // ---------------- MÉTHODES PRINCIPALES ----------------

    /**
     * Définit les scores et clôt le match.
     */
    public void definirScores(int score1, int score2) {
        if (statut == Statut.CLOS) {
            throw new IllegalStateException("Match déjà clos");
        }
        if (score1 < 0 || score2 < 0) {
            throw new IllegalArgumentException("Les scores doivent être positifs");
        }

        setScoreEquipe1(score1);
        setScoreEquipe2(score2);

        cloreMatch();
    }

    private void cloreMatch() {
        this.statut = Statut.CLOS;
    }

    // ---------------- PERSISTENCE ----------------

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO matchs (ronde_id, terrain_id, score_e1, score_e2, statut)
            VALUES (?, ?, ?, ?, ?)
            """;
        PreparedStatement ps =
            con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, ronde.getId());
        if (terrain != null) {
            ps.setInt(2, terrain.getId());
        } else {
            ps.setNull(2, java.sql.Types.INTEGER);
        }
        ps.setInt(3, this.getScoreEquipe1());
        ps.setInt(4, this.getScoreEquipe2());
        ps.setString(5, this.statut.name());

        ps.executeUpdate();   // important

        return ps;
    }

    /**
     * Mise à jour du match (scores + statut + terrain).
     */
    public void updateInDB(Connection con) throws SQLException {
        String sql = """
            UPDATE matchs
            SET terrain_id = ?, score_e1 = ?, score_e2 = ?, statut = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (terrain != null) {
                ps.setInt(1, terrain.getId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setInt(2, this.getScoreEquipe1());
            ps.setInt(3, this.getScoreEquipe2());
            ps.setString(4, this.statut.name());
            ps.setInt(5, this.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Suppression d'un match en BD.
     */
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
        return "Match sur " + nomTerrain + " : "
                + "E1=" + equipe1.getTailleActuelle() + " joueurs, score=" + getScoreEquipe1()
                + " | E2=" + equipe2.getTailleActuelle() + " joueurs, score=" + getScoreEquipe2()
                + " (" + statut + ")";
    }
}
