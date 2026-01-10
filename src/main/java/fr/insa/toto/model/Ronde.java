package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ronde extends ClasseMiroir {

    // --- attributs BD ---
    private int idTournoi;
    private int numero;
    private LocalDateTime debut;
    private boolean close;

    // --- côté Java (optionnel) ---
    private final List<Match> matchs = new ArrayList<>();

    // Ronde nouvelle (pas encore en BD)
    public Ronde(int idTournoi, int numero) {
        super();
        this.idTournoi = idTournoi;
        this.numero = numero;
        this.debut = LocalDateTime.now();
        this.close = false;
    }

    // Ronde depuis BD
    public Ronde(int id, int idTournoi, int numero, LocalDateTime debut, boolean close) {
        super(id);
        this.idTournoi = idTournoi;
        this.numero = numero;
        this.debut = debut;
        this.close = close;
    }

    // ---------- getters ----------
    public int getIdTournoi() { return idTournoi; }
    public int getNumero() { return numero; }
    public LocalDateTime getDebut() { return debut; }
    public boolean isClose() { return close; }

    public void clore() { this.close = true; }

    public List<Match> getMatchs() { return matchs; }

    public void ajouterMatch(Match m) {
        if (close) throw new IllegalStateException("Ronde close");
        if (m == null) throw new IllegalArgumentException("Match null");
        matchs.add(m);
    }

    // ---------- INSERT ----------
    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO ronde (id_tournoi, numero, debut, close)
            VALUES (?, ?, ?, ?)
            """;
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, idTournoi);
        ps.setInt(2, numero);
        ps.setTimestamp(3, Timestamp.valueOf(debut));
        ps.setBoolean(4, close);
        ps.executeUpdate();
        return ps;
    }

    // ---------- UPDATE (close) ----------
    public void updateInDB(Connection con) throws SQLException {
        if (getId() < 0) throw new IllegalStateException("Ronde sans id");
        String sql = "UPDATE ronde SET close = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, close);
            ps.setInt(2, getId());
            ps.executeUpdate();
        }
    }

    // ---------- GET BY ID ----------
    public static Ronde getInDB(Connection con, int id) throws SQLException {
        String sql = """
            SELECT id, id_tournoi, numero, debut, close
            FROM ronde
            WHERE id = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Timestamp ts = rs.getTimestamp("debut");
                LocalDateTime debut = (ts == null) ? null : ts.toLocalDateTime();
                return new Ronde(
                    rs.getInt("id"),
                    rs.getInt("id_tournoi"),
                    rs.getInt("numero"),
                    debut,
                    rs.getBoolean("close")
                );
            }
        }
    }

    // ---------- DERNIERE RONDE ----------
    public static Ronde findDerniereRonde(Connection con, int idTournoi) throws SQLException {
        String sql = """
            SELECT id, id_tournoi, numero, debut, close
            FROM ronde
            WHERE id_tournoi = ?
            ORDER BY numero DESC
            LIMIT 1
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTournoi);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Timestamp ts = rs.getTimestamp("debut");
                LocalDateTime debut = (ts == null) ? null : ts.toLocalDateTime();
                return new Ronde(
                    rs.getInt("id"),
                    rs.getInt("id_tournoi"),
                    rs.getInt("numero"),
                    debut,
                    rs.getBoolean("close")
                );
            }
        }
    }

    public static Ronde creeRondeAuto(Connection con, int idTournoi) throws SQLException {

    Tournoi t = Tournoi.getInDB(con, idTournoi);
    if (t == null) {
        throw new SQLException("Tournoi introuvable id=" + idTournoi);
    }

    // Prochain numéro
    int prochainNumero = 1;
    String sqlNum = "SELECT COALESCE(MAX(numero), 0) + 1 FROM ronde WHERE id_tournoi = ?";
    try (PreparedStatement ps = con.prepareStatement(sqlNum)) {
        ps.setInt(1, idTournoi);
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            prochainNumero = rs.getInt(1);
        }
    }

    // Création + insertion
    Ronde r = new Ronde(idTournoi, prochainNumero);
    r.saveInDB(con); // récupère son id
    return r;
}

    // ---------- LISTE RONDES D'UN TOURNOI ----------
    public static List<Ronde> toutesLesRondesDuTournoi(Connection con, int idTournoi) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        String sql = """
            SELECT id
            FROM ronde
            WHERE id_tournoi = ?
            ORDER BY numero
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTournoi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(getInDB(con, rs.getInt("id")));
                }
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "Ronde #" + numero + " (id=" + getId() + ", close=" + close + ")";
    }
}
