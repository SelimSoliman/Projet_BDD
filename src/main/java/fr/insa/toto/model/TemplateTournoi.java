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
 * Extension 6 : Facilité de création d'un nouveau tournoi
 * Permet de créer des templates/modèles de tournois réutilisables
 */
public class TemplateTournoi extends ClasseMiroir {

    private String nom;
    private String description;
    private int nbTerrains;
    private int nbJoueursParEquipe;
    private int dureeRondeMinutes;
    private boolean publi;  // Template public (visible par tous) ou privé

    // Relations
    private List<Integer> terrainsIds = new ArrayList<>();
    private List<Integer> typesJeuIds = new ArrayList<>();

    // --- Constructeurs ---
    public TemplateTournoi(String nom, String description, int nbTerrains, 
                          int nbJoueursParEquipe, int dureeRondeMinutes, boolean publi) {
        super();
        this.nom = nom;
        this.description = description;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
        this.dureeRondeMinutes = dureeRondeMinutes;
        this.publi = publi;
    }

    public TemplateTournoi(int id, String nom, String description, int nbTerrains,
                          int nbJoueursParEquipe, int dureeRondeMinutes, boolean publi) {
        super(id);
        this.nom = nom;
        this.description = description;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
        this.dureeRondeMinutes = dureeRondeMinutes;
        this.publi = publi;
    }

    // --- Création du schéma ---
    public static void creerSchemaTemplate(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            // Table des templates
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS template_tournoi (
                    id INTEGER AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(200) NOT NULL,
                    description TEXT,
                    nb_terrains INTEGER NOT NULL,
                    nb_joueurs_par_equipe INTEGER NOT NULL,
                    duree_ronde_minutes INTEGER DEFAULT 20,
                    public BOOLEAN DEFAULT FALSE,
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Association template <-> terrains
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS template_terrain (
                    id_template INTEGER NOT NULL,
                    id_terrain INTEGER NOT NULL,
                    PRIMARY KEY (id_template, id_terrain),
                    FOREIGN KEY (id_template) REFERENCES template_tournoi(id),
                    FOREIGN KEY (id_terrain) REFERENCES terrain(id)
                )
                """);

            // Association template <-> types de jeu
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS template_type_jeu (
                    id_template INTEGER NOT NULL,
                    id_type_jeu INTEGER NOT NULL,
                    PRIMARY KEY (id_template, id_type_jeu),
                    FOREIGN KEY (id_template) REFERENCES template_tournoi(id),
                    FOREIGN KEY (id_type_jeu) REFERENCES type_jeu(id)
                )
                """);
        }
    }

    // --- Persistance ---
    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO template_tournoi 
                (nom, description, nb_terrains, nb_joueurs_par_equipe, 
                 duree_ronde_minutes, public)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, this.nom);
        ps.setString(2, this.description);
        ps.setInt(3, this.nbTerrains);
        ps.setInt(4, this.nbJoueursParEquipe);
        ps.setInt(5, this.dureeRondeMinutes);
        ps.setBoolean(6, this.publi);
        ps.executeUpdate();
        return ps;
    }

    /**
     * Sauvegarde complète du template avec ses associations
     * IMPORTANT : Appeler cette méthode une seule fois après avoir ajouté 
     * tous les terrains et types de jeu
     */
    public void saveComplete(Connection con) throws SQLException {
        // Sauver le template lui-même si pas encore sauvegardé
        if (this.getId() == -1) {
            this.saveInDB(con);
        }

        // Nettoyer les associations existantes pour éviter les doublons
        String sqlDeleteTerrains = "DELETE FROM template_terrain WHERE id_template = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlDeleteTerrains)) {
            ps.setInt(1, this.getId());
            ps.executeUpdate();
        }

        String sqlDeleteTypeJeu = "DELETE FROM template_type_jeu WHERE id_template = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlDeleteTypeJeu)) {
            ps.setInt(1, this.getId());
            ps.executeUpdate();
        }

        // Sauver les associations terrains
        if (!terrainsIds.isEmpty()) {
            String sqlTerrain = "INSERT INTO template_terrain (id_template, id_terrain) VALUES (?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sqlTerrain)) {
                for (Integer idTerrain : terrainsIds) {
                    ps.setInt(1, this.getId());
                    ps.setInt(2, idTerrain);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // Sauver les associations types de jeu
        if (!typesJeuIds.isEmpty()) {
            String sqlTypeJeu = "INSERT INTO template_type_jeu (id_template, id_type_jeu) VALUES (?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sqlTypeJeu)) {
                for (Integer idTypeJeu : typesJeuIds) {
                    ps.setInt(1, this.getId());
                    ps.setInt(2, idTypeJeu);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    // --- Requêtes ---
    
    /**
     * Récupère tous les templates publics + privés de l'utilisateur
     */
    public static List<TemplateTournoi> getTousLesTemplates(Connection con) throws SQLException {
        List<TemplateTournoi> result = new ArrayList<>();
        String sql = """
            SELECT id, nom, description, nb_terrains, nb_joueurs_par_equipe, 
                   duree_ronde_minutes, public
            FROM template_tournoi
            ORDER BY date_creation DESC
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TemplateTournoi t = new TemplateTournoi(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getInt("nb_terrains"),
                    rs.getInt("nb_joueurs_par_equipe"),
                    rs.getInt("duree_ronde_minutes"),
                    rs.getBoolean("public")
                );
                
                // Charger les terrains associés
                t.terrainsIds = getTerrainIdsForTemplate(con, t.getId());
                
                // Charger les types de jeu associés
                t.typesJeuIds = getTypeJeuIdsForTemplate(con, t.getId());
                
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Récupère un template par son ID
     */
    public static TemplateTournoi getById(Connection con, int id) throws SQLException {
        String sql = """
            SELECT id, nom, description, nb_terrains, nb_joueurs_par_equipe,
                   duree_ronde_minutes, public
            FROM template_tournoi
            WHERE id = ?
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TemplateTournoi t = new TemplateTournoi(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getInt("nb_terrains"),
                        rs.getInt("nb_joueurs_par_equipe"),
                        rs.getInt("duree_ronde_minutes"),
                        rs.getBoolean("public")
                    );
                    
                    t.terrainsIds = getTerrainIdsForTemplate(con, id);
                    t.typesJeuIds = getTypeJeuIdsForTemplate(con, id);
                    
                    return t;
                }
            }
        }
        return null;
    }

    private static List<Integer> getTerrainIdsForTemplate(Connection con, int templateId) 
            throws SQLException {
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT id_terrain FROM template_terrain WHERE id_template = ?";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("id_terrain"));
                }
            }
        }
        return result;
    }

    private static List<Integer> getTypeJeuIdsForTemplate(Connection con, int templateId) 
            throws SQLException {
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT id_type_jeu FROM template_type_jeu WHERE id_template = ?";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("id_type_jeu"));
                }
            }
        }
        return result;
    }

    // --- Création de tournoi depuis template ---

    /**
     * Crée un nouveau tournoi à partir de ce template
     */
    public TournoiMulti creerTournoiDepuisTemplate(Connection con, String nomNouveauTournoi) 
            throws SQLException {
        
        // Créer le tournoi
        TournoiMulti nouveau = new TournoiMulti(
            nomNouveauTournoi,
            this.nbTerrains,
            this.nbJoueursParEquipe
        );
        nouveau.saveInDB(con);

        // Note : Les terrains devront être chargés séparément après création
        // car TournoiMulti n'a pas de liste de terrains en mémoire dans sa version actuelle

        return nouveau;
    }

    /**
     * Clone ce template avec un nouveau nom
     */
    public TemplateTournoi cloner(Connection con, String nouveauNom) throws SQLException {
        TemplateTournoi clone = new TemplateTournoi(
            nouveauNom,
            this.description,
            this.nbTerrains,
            this.nbJoueursParEquipe,
            this.dureeRondeMinutes,
            false  // Le clone est privé par défaut
        );
        
        clone.terrainsIds = new ArrayList<>(this.terrainsIds);
        clone.typesJeuIds = new ArrayList<>(this.typesJeuIds);
        clone.saveComplete(con);
        
        return clone;
    }

    private Terrain getTerrain(Connection con, int id) throws SQLException {
        String sql = "SELECT nom, disponible FROM terrain WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Terrain(id, rs.getString("nom"), rs.getBoolean("disponible"));
                }
            }
        }
        return null;
    }

    // --- Templates prédéfinis ---

    /**
     * Crée des templates d'exemple
     */
    public static void creerTemplatesExemples(Connection con) throws SQLException {
        // Template Tennis
        TemplateTournoi tennis = new TemplateTournoi(
            "Tournoi Tennis Standard",
            "Configuration standard pour un tournoi de tennis avec 4 courts",
            4,
            2,  // Double
            30, // 30 minutes par match
            true
        );
        tennis.saveInDB(con);

        // Template Football
        TemplateTournoi football = new TemplateTournoi(
            "Tournoi Football 5v5",
            "Tournoi de football en salle, équipes de 5",
            2,
            5,
            15, // 15 minutes par match
            true
        );
        football.saveInDB(con);

        // Template Basket
        TemplateTournoi basket = new TemplateTournoi(
            "Tournoi Basketball",
            "Tournoi de basketball 3x3",
            3,
            3,
            10, // 10 minutes par match
            true
        );
        basket.saveInDB(con);
    }

    // --- Gestion des associations ---

    public void ajouterTerrain(int idTerrain) {
        if (!terrainsIds.contains(idTerrain)) {
            terrainsIds.add(idTerrain);
        }
    }

    public void retirerTerrain(int idTerrain) {
        terrainsIds.remove(Integer.valueOf(idTerrain));
    }

    public void ajouterTypeJeu(int idTypeJeu) {
        if (!typesJeuIds.contains(idTypeJeu)) {
            typesJeuIds.add(idTypeJeu);
        }
    }

    public void retirerTypeJeu(int idTypeJeu) {
        typesJeuIds.remove(Integer.valueOf(idTypeJeu));
    }

    // --- Getters / Setters ---
    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getNbTerrains() { return nbTerrains; }
    public void setNbTerrains(int nbTerrains) { this.nbTerrains = nbTerrains; }
    
    public int getNbJoueursParEquipe() { return nbJoueursParEquipe; }
    public void setNbJoueursParEquipe(int nbJoueursParEquipe) { 
        this.nbJoueursParEquipe = nbJoueursParEquipe; 
    }
    
    public int getDureeRondeMinutes() { return dureeRondeMinutes; }
    public void setDureeRondeMinutes(int dureeRondeMinutes) { 
        this.dureeRondeMinutes = dureeRondeMinutes; 
    }
    
    public boolean isPublic() { return publi; }
    public void setPublic(boolean publi) { this.publi = publi; }
    
    public List<Integer> getTerrainsIds() { return new ArrayList<>(terrainsIds); }
    public List<Integer> getTypesJeuIds() { return new ArrayList<>(typesJeuIds); }

    @Override
    public String toString() {
        return String.format("%s (%d terrains, %d joueurs/équipe, %dmin/ronde)%s",
                           nom, nbTerrains, nbJoueursParEquipe, dureeRondeMinutes,
                           publi ? " [PUBLIC]" : "");
    }
}