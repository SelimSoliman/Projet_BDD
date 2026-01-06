
package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class TypeJeu extends ClasseMiroir {

    private String nom;
    private int nbEquipes;
    private int nbJoueursMin;    // Nombre minimum de joueurs par équipe
    private int nbJoueursMax;    // Nombre maximum de joueurs par équipe
    private boolean tailleEquipeVariable;  // true si le nombre de joueurs par équipe peut varier

    // --- Constructeurs ---
    public TypeJeu(String nom, int nbEquipes, int nbJoueursMin, int nbJoueursMax) {
        super();
        this.nom = nom;
        this.nbEquipes = nbEquipes;
        this.nbJoueursMin = nbJoueursMin;
        this.nbJoueursMax = nbJoueursMax;
        this.tailleEquipeVariable = (nbJoueursMin != nbJoueursMax);
    }

    public TypeJeu(int id, String nom, int nbEquipes, int nbJoueursMin, int nbJoueursMax) {
        super(id);
        this.nom = nom;
        this.nbEquipes = nbEquipes;
        this.nbJoueursMin = nbJoueursMin;
        this.nbJoueursMax = nbJoueursMax;
        this.tailleEquipeVariable = (nbJoueursMin != nbJoueursMax);
    }

    // --- Création du schéma ---
    public static void creerSchemTypeJeu(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            // Table des types de jeux
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS type_jeu (
                    id INTEGER AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(100) NOT NULL UNIQUE,
                    nb_equipes INTEGER NOT NULL,
                    nb_joueurs_min INTEGER NOT NULL,
                    nb_joueurs_max INTEGER NOT NULL,
                    CHECK (nb_joueurs_min > 0),
                    CHECK (nb_joueurs_max >= nb_joueurs_min),
                    CHECK (nb_equipes >= 2)
                )
                """);

            // Table d'association terrain <-> type de jeu
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS terrain_type_jeu (
                    id_terrain INTEGER NOT NULL,
                    id_type_jeu INTEGER NOT NULL,
                    PRIMARY KEY (id_terrain, id_type_jeu),
                    FOREIGN KEY (id_terrain) REFERENCES terrain(id),
                    FOREIGN KEY (id_type_jeu) REFERENCES type_jeu(id)
                )
                """);

            // Modifier la table matchs pour ajouter le type de jeu
            st.executeUpdate("""
                ALTER TABLE matchs ADD COLUMN IF NOT EXISTS 
                    id_type_jeu INTEGER,
                    FOREIGN KEY (id_type_jeu) REFERENCES type_jeu(id)
                """);
        }
    }

    // --- Persistance ---
    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO type_jeu (nom, nb_equipes, nb_joueurs_min, nb_joueurs_max)
            VALUES (?, ?, ?, ?)
            """;
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, this.nom);
        ps.setInt(2, this.nbEquipes);
        ps.setInt(3, this.nbJoueursMin);
        ps.setInt(4, this.nbJoueursMax);
        ps.executeUpdate();
        return ps;
    }

    // --- Méthodes de requête ---
    public static List<TypeJeu> tousLesTypesJeu(Connection con) throws SQLException {
        List<TypeJeu> result = new ArrayList<>();
        String sql = "SELECT id, nom, nb_equipes, nb_joueurs_min, nb_joueurs_max FROM type_jeu";
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new TypeJeu(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getInt("nb_equipes"),
                    rs.getInt("nb_joueurs_min"),
                    rs.getInt("nb_joueurs_max")
                ));
            }
        }
        return result;
    }

    /**
     * Associe un type de jeu à un terrain
     */
    public void associerTerrain(Connection con, int idTerrain) throws SQLException {
        String sql = "INSERT INTO terrain_type_jeu (id_terrain, id_type_jeu) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTerrain);
            ps.setInt(2, this.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Récupère tous les types de jeu possibles sur un terrain donné
     */
    public static List<TypeJeu> getTypesJeuPourTerrain(Connection con, int idTerrain) throws SQLException {
        List<TypeJeu> result = new ArrayList<>();
        String sql = """
            SELECT tj.id, tj.nom, tj.nb_equipes, tj.nb_joueurs_min, tj.nb_joueurs_max
            FROM type_jeu tj
            JOIN terrain_type_jeu ttj ON ttj.id_type_jeu = tj.id
            WHERE ttj.id_terrain = ?
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTerrain);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new TypeJeu(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("nb_equipes"),
                        rs.getInt("nb_joueurs_min"),
                        rs.getInt("nb_joueurs_max")
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Crée des types de jeu prédéfinis (exemples)
     */
    public static void creerTypesJeuExemples(Connection con) throws SQLException {
        List<TypeJeu> exemples = List.of(
            new TypeJeu("Tennis Simple", 2, 1, 1),
            new TypeJeu("Tennis Double", 2, 2, 2),
            new TypeJeu("Football", 2, 11, 11),
            new TypeJeu("Basketball", 2, 5, 5),
            new TypeJeu("Tarot", 4, 1, 1),
            new TypeJeu("Trivial Pursuit", 3, 1, 3),  // taille équipe variable
            new TypeJeu("Volley-ball", 2, 6, 6),
            new TypeJeu("Badminton Simple", 2, 1, 1),
            new TypeJeu("Badminton Double", 2, 2, 2)
        );

        for (TypeJeu tj : exemples) {
            tj.saveInDB(con);
        }
    }

    /**
     * Valide qu'une répartition de joueurs est correcte pour ce type de jeu
     */
    public boolean validerRepartitionJoueurs(List<List<Joueur>> equipes) {
        if (equipes.size() != this.nbEquipes) {
            return false;
        }

        for (List<Joueur> equipe : equipes) {
            int taille = equipe.size();
            if (taille < nbJoueursMin || taille > nbJoueursMax) {
                return false;
            }
        }

        return true;
    }

    // --- Getters / Setters ---
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public int getNbEquipes() { return nbEquipes; }
    public void setNbEquipes(int nbEquipes) { this.nbEquipes = nbEquipes; }
    
    public int getNbJoueursMin() { return nbJoueursMin; }
    public void setNbJoueursMin(int nbJoueursMin) { 
        this.nbJoueursMin = nbJoueursMin; 
        this.tailleEquipeVariable = (nbJoueursMin != nbJoueursMax);
    }
    
    public int getNbJoueursMax() { return nbJoueursMax; }
    public void setNbJoueursMax(int nbJoueursMax) { 
        this.nbJoueursMax = nbJoueursMax;
        this.tailleEquipeVariable = (nbJoueursMin != nbJoueursMax);
    }
    
    public boolean isTailleEquipeVariable() { return tailleEquipeVariable; }

    @Override
    public String toString() {
        if (tailleEquipeVariable) {
            return String.format("%s (%d équipes de %d-%d joueurs)", 
                               nom, nbEquipes, nbJoueursMin, nbJoueursMax);
        } else {
            return String.format("%s (%d équipes de %d joueurs)", 
                               nom, nbEquipes, nbJoueursMin);
        }
    }
}