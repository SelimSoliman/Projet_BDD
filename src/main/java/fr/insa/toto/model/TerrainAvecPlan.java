package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension 3 : Gestion simple des terrains avec plan
 * Terrain étendu avec possibilité d'associer un plan (image/PDF)
 */
public class TerrainAvecPlan extends Terrain {

    private String cheminPlan;  // Chemin vers l'image ou PDF du plan
    private String description;

    // --- Constructeurs ---
    public TerrainAvecPlan(String nom, String description) {
        super(nom);
        this.description = description;
    }

    public TerrainAvecPlan(int id, String nom, boolean disponible, 
                          String cheminPlan, String description) {
        super(id, nom, disponible);
        this.cheminPlan = cheminPlan;
        this.description = description;
    }

    // --- Modification du schéma ---
    public static void ajouterColonnesPlan(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("""
                ALTER TABLE terrain ADD COLUMN IF NOT EXISTS 
                    chemin_plan VARCHAR(500),
                    description TEXT
                """);
        }
    }

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = "INSERT INTO terrain (nom, disponible, chemin_plan, description) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, getNom());
        ps.setBoolean(2, estDisponible());
        ps.setString(3, this.cheminPlan);
        ps.setString(4, this.description);
        ps.executeUpdate();
        return ps;
    }

    public void updatePlan(Connection con, String cheminPlan) throws SQLException {
        this.cheminPlan = cheminPlan;
        String sql = "UPDATE terrain SET chemin_plan = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cheminPlan);
            ps.setInt(2, getId());
            ps.executeUpdate();
        }
    }

    public static List<TerrainAvecPlan> tousLesTerrainsAvecPlan(Connection con) throws SQLException {
        List<TerrainAvecPlan> result = new ArrayList<>();
        String sql = "SELECT id, nom, disponible, chemin_plan, description FROM terrain ORDER BY id";
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new TerrainAvecPlan(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getBoolean("disponible"),
                    rs.getString("chemin_plan"),
                    rs.getString("description")
                ));
            }
        }
        return result;
    }

    // --- Getters / Setters ---
    public String getCheminPlan() { return cheminPlan; }
    public void setCheminPlan(String cheminPlan) { this.cheminPlan = cheminPlan; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

