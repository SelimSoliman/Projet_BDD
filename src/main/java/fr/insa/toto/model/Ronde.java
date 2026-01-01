package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ronde extends ClasseMiroir {

    // --- attributs BD ---
    private Tournoi tournoi;              // tournoi auquel appartient la ronde
    private int numero;                   // place dans le tournoi : 1, 2, 3...
    private LocalDateTime debut;          // timestamp de debut
    private boolean close = false;        // statut close / en cours

    // --- cote Java ---
    private List<Match> matchs = new ArrayList<>();

    // Ronde nouvelle (non encore sauvegardee)
    public Ronde(Tournoi tournoi, int numero) {
        super();                    // id = -1
        this.tournoi = tournoi;
        this.numero = numero;
        this.debut = LocalDateTime.now();
    }

    // Ronde recuperee depuis la BD (si besoin)
    public Ronde(int id, Tournoi tournoi, int numero,
                 LocalDateTime debut, boolean close) {
        super(id);
        this.tournoi = tournoi;
        this.numero = numero;
        this.debut = debut;
        this.close = close;
    }

    // ----------- GETTERS / SETTERS -----------

    public Tournoi getTournoi() { return tournoi; }

    public int getNumero() { return numero; }

    public LocalDateTime getDebut() { return debut; }

    public boolean isClose() { return close; }

    public void clore() { this.close = true; }

    public List<Match> getMatchs() { return matchs; }

    public void ajouterMatch(Match m) {
        if (close) {
            throw new IllegalStateException("Impossible d'ajouter un match : ronde deja close");
        }
        if (m == null) {
            throw new IllegalArgumentException("Match null");
        }
        matchs.add(m);
    }

    // ----------- PERSISTENCE -----------

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO ronde (id_tournoi, numero, debut, close)
            VALUES (?, ?, ?, ?)
            """;
        PreparedStatement ps =
            con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, tournoi.getId());
        ps.setInt(2, this.numero);
        ps.setTimestamp(3, java.sql.Timestamp.valueOf(this.debut));
        ps.setBoolean(4, this.close);

        ps.executeUpdate();   // execute l'INSERT

        return ps;
    }
public void updateInDB(Connection con) throws SQLException {
    if (this.getId() < 0) {
        throw new IllegalStateException("Ronde sans id : impossible de faire update");
    }
    String sql = "update ronde set close = ? where id = ?";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setBoolean(1, this.isClose());
        ps.setInt(2, this.getId());
        ps.executeUpdate();
    }
}


    @Override
    public String toString() {
        return "Ronde " + numero
                + " (debut : " + debut
                + ", " + (close ? "close" : "en cours")
                + ", " + matchs.size() + " matchs)";
    }
}
