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

    private int id;            // Identifiant BD
    private Ronde ronde;       // Ronde à laquelle appartient ce match

    private Equipe equipe1;
    private Equipe equipe2;

    private Statut statut = Statut.EN_COURS;

    public Match(int id, Ronde ronde) {
        this.id = id;
        this.ronde = ronde;
        this.equipe1 = new Equipe(this, 1);
        this.equipe2 = new Equipe(this, 2);
    }

    // ---------------- GETTERS ----------------

    public int getId() { return id; }

    public Ronde getRonde() { return ronde; }

    public Equipe getEquipe1() { return equipe1; }

    public Equipe getEquipe2() { return equipe2; }

    public int getScoreEquipe1() { return equipe1.getScoreTotal(); }

    public int getScoreEquipe2() { return equipe2.getScoreTotal(); }

    public Statut getStatut() { return statut; }

    // ---------------- MÉTHODES PRINCIPALES ----------------

    public void definirScores(int score1, int score2) {
        if (statut == Statut.CLOS) {
            throw new IllegalStateException("Match déjà clos");
        }

        equipe1.ajouterScore(score1);
        equipe2.ajouterScore(score2);

        cloreMatch();
    }

    private void cloreMatch() {
        this.statut = Statut.CLOS;

        // Chaque joueur de l'équipe reçoit le même score
        for (Joueur j : equipe1.getJoueurs()) {
            j.ajouterScore(equipe1.getScoreTotal());
        }
        for (Joueur j : equipe2.getJoueurs()) {
            j.ajouterScore(equipe2.getScoreTotal());
        }
    }

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = "INSERT INTO Matchs (ronde_id, score_e1, score_e2, statut) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, ronde.getId());
        ps.setInt(2, this.getScoreEquipe1());
        ps.setInt(3, this.getScoreEquipe2());
        ps.setString(4, this.statut.name());

        return ps;
    }

    @Override
    public String toString() {
        return "Match : E1=" + equipe1.getTailleActuelle() + " joueurs, score=" + getScoreEquipe1() +
               " | E2=" + equipe2.getTailleActuelle() + " joueurs, score=" + getScoreEquipe2() +
               " (" + statut + ")";
    }
}
