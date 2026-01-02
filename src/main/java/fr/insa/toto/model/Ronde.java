package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



public class Ronde extends ClasseMiroir {
    
public static boolean tryCloseRonde(Connection con, int rondeId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM matchs WHERE ronde_id = ? AND statut = ?";
    int nbEnCours = 0;
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, rondeId);
        ps.setString(2, Match.Statut.EN_COURS.name());
        try (ResultSet rs = ps.executeQuery()) {
           if (rs.next()) nbEnCours = rs.getInt(1);

        }
    }

    if (nbEnCours == 0) {
        try (PreparedStatement ps2 = con.prepareStatement("UPDATE ronde SET close = TRUE WHERE id = ?")) {
            ps2.setInt(1, rondeId);
            ps2.executeUpdate();
        }
        return true;
    }
    return false;
}


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
     public static Ronde findDerniereRonde(Connection con, Tournoi t) throws SQLException {
    String sql = """
        select id, numero, debut, close
        from ronde
        where id_tournoi = ?
        order by numero desc
        limit 1
        """;
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, t.getId());
        var rs = ps.executeQuery();
        if (rs.next()) {
            return new Ronde(
                rs.getInt("id"),
                t,
                rs.getInt("numero"),
                rs.getTimestamp("debut").toLocalDateTime(),
                rs.getBoolean("close")
            );
        } else {
            return null;
        }
    }
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
   public static List<Ronde> toutesLesRondes(Connection con, Tournoi t) throws SQLException {
    List<Ronde> res = new ArrayList<>();
    String sql = """
        SELECT id, numero, debut, close
        FROM ronde
        WHERE id_tournoi = ?
        ORDER BY numero
        """;
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, t.getId());
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Ronde r = new Ronde(
                        rs.getInt("id"),
                        t,
                        rs.getInt("numero"),
                        rs.getTimestamp("debut").toLocalDateTime(),
                        rs.getBoolean("close")
                );
                res.add(r);
            }
        }
    }
    return res;
}


    


}
