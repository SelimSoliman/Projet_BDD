package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Terrain extends ClasseMiroir {

    private String nom;
    private boolean disponible;

    // --- Constructeurs ---

    // Nouveau terrain en mémoire
    public Terrain(String nom) {
        super();          // id = -1
        setNom(nom);
        this.disponible = true;
    }

    // Terrain récupéré depuis la BD
    public Terrain(int id, String nom, boolean disponible) {
        super(id);
        setNom(nom);
        this.disponible = disponible;
    }

    // --- Getters / Setters ---

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }
        this.nom = nom;
    }

    public boolean estDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    // --- Méthodes métier ---

    public void occuper() {
        if (!disponible) {
            throw new IllegalStateException("Le terrain est déjà occupé");
        }
        this.disponible = false;
    }

    public void liberer() {
        if (disponible) {
            throw new IllegalStateException("Le terrain est déjà libre");
        }
        this.disponible = true;
    }

    public void basculerDisponibilite() {
        this.disponible = !this.disponible;
    }

    // --- Persistance ---

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = "INSERT INTO terrain (nom, disponible) VALUES (?, ?)";
        PreparedStatement ps =
            con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, this.nom);
        ps.setBoolean(2, this.disponible);

        ps.executeUpdate();    // exécute l'INSERT

        return ps;
    }

    @Override
    public String toString() {
        return "Terrain " + getId() + " : " + nom +
               " - " + (disponible ? "Disponible" : "Occupé");
    }
public static void supprimer(Connection con, int id) throws SQLException {
    String sql = "delete from terrain where id = ?";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Terrain terrain = (Terrain) obj;
        return this.getId() == terrain.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }
}
