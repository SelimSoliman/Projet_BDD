package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extension 2 : Gestion multi-tournoi
 * Version étendue de Tournoi qui permet de gérer plusieurs tournois
 * avec historique et classement global des joueurs
 */
public class TournoiMulti extends ClasseMiroir {

    // --- Statuts possibles d'un tournoi ---
    public enum StatutTournoi {
        EN_PREPARATION,  // Le tournoi est créé mais pas encore démarré
        EN_COURS,        // Le tournoi a démarré et des rondes sont en cours
        TERMINE          // Le tournoi est terminé
    }

    // --- Attributs BD ---
    private String nom;
    private int nbTerrains;
    private int nbJoueursParEquipe;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private StatutTournoi statut;

    // --- Collections (mémoire) ---
    private final List<Joueur> joueurs = new ArrayList<>();
    private final List<Ronde> rondes = new ArrayList<>();
    private final List<Terrain> terrains = new ArrayList<>();

    // --- Constructeurs ---
    public TournoiMulti(String nom, int nbTerrains, int nbJoueursParEquipe) {
        super();
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
        this.dateCreation = LocalDateTime.now();
        this.statut = StatutTournoi.EN_PREPARATION;
    }

    public TournoiMulti(int id, String nom, int nbTerrains, int nbJoueursParEquipe,
                        LocalDateTime dateCreation, LocalDateTime dateDebut, 
                        LocalDateTime dateFin, StatutTournoi statut) {
        super(id);
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
        this.dateCreation = dateCreation;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
    }

    // ======================
    // MODIFICATIONS DU SCHEMA
    // ======================
    
    /**
     * Crée le schéma étendu pour multi-tournoi
     */
    public static void creerSchemaMultiTournoi(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            // Modifier la table tournoi pour ajouter les nouveaux champs
            st.executeUpdate("""
                ALTER TABLE tournoi ADD COLUMN IF NOT EXISTS 
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    date_debut TIMESTAMP,
                    date_fin TIMESTAMP,
                    statut VARCHAR(20) DEFAULT 'EN_PREPARATION'
                """);

            // Table pour l'inscription des joueurs à un tournoi spécifique
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS inscription_tournoi (
                    id_tournoi INTEGER NOT NULL,
                    id_joueur INTEGER NOT NULL,
                    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id_tournoi, id_joueur),
                    FOREIGN KEY (id_tournoi) REFERENCES tournoi(id),
                    FOREIGN KEY (id_joueur) REFERENCES joueur(id)
                )
                """);

            // Table pour le classement global des joueurs (tous tournois)
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS classement_global (
                    id_joueur INTEGER PRIMARY KEY,
                    nb_tournois_participes INTEGER DEFAULT 0,
                    nb_matchs_joues INTEGER DEFAULT 0,
                    nb_victoires INTEGER DEFAULT 0,
                    score_total INTEGER DEFAULT 0,
                    derniere_mise_a_jour TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (id_joueur) REFERENCES joueur(id)
                )
                """);
        }
    }

    // ======================
    // PERSISTANCE
    // ======================

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO tournoi (nom, nb_terrains, nb_joueurs_par_equipe, 
                                date_creation, date_debut, date_fin, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, this.nom);
        ps.setInt(2, this.nbTerrains);
        ps.setInt(3, this.nbJoueursParEquipe);
        ps.setTimestamp(4, java.sql.Timestamp.valueOf(this.dateCreation));
        ps.setTimestamp(5, this.dateDebut != null ? java.sql.Timestamp.valueOf(this.dateDebut) : null);
        ps.setTimestamp(6, this.dateFin != null ? java.sql.Timestamp.valueOf(this.dateFin) : null);
        ps.setString(7, this.statut.name());
        ps.executeUpdate();
        return ps;
    }

    public void updateInDB(Connection con) throws SQLException {
        if (this.getId() < 0) {
            throw new IllegalStateException("Tournoi sans id");
        }
        String sql = """
            UPDATE tournoi
            SET nom = ?, nb_terrains = ?, nb_joueurs_par_equipe = ?,
                date_debut = ?, date_fin = ?, statut = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, this.nom);
            ps.setInt(2, this.nbTerrains);
            ps.setInt(3, this.nbJoueursParEquipe);
            ps.setTimestamp(4, this.dateDebut != null ? java.sql.Timestamp.valueOf(this.dateDebut) : null);
            ps.setTimestamp(5, this.dateFin != null ? java.sql.Timestamp.valueOf(this.dateFin) : null);
            ps.setString(6, this.statut.name());
            ps.setInt(7, this.getId());
            ps.executeUpdate();
        }
    }

    // ======================
    // GESTION MULTI-TOURNOI
    // ======================

    /**
     * Récupère tous les tournois (en cours, terminés, en préparation)
     */
    public static List<TournoiMulti> tousLesTournois(Connection con) throws SQLException {
        List<TournoiMulti> result = new ArrayList<>();
        String sql = """
            SELECT id, nom, nb_terrains, nb_joueurs_par_equipe,
                   date_creation, date_debut, date_fin, statut
            FROM tournoi
            ORDER BY date_creation DESC
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new TournoiMulti(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getInt("nb_terrains"),
                    rs.getInt("nb_joueurs_par_equipe"),
                    rs.getTimestamp("date_creation").toLocalDateTime(),
                    rs.getTimestamp("date_debut") != null 
                        ? rs.getTimestamp("date_debut").toLocalDateTime() : null,
                    rs.getTimestamp("date_fin") != null 
                        ? rs.getTimestamp("date_fin").toLocalDateTime() : null,
                    StatutTournoi.valueOf(rs.getString("statut"))
                ));
            }
        }
        return result;
    }

    /**
     * Récupère un tournoi par son ID
     */
    public static TournoiMulti getById(Connection con, int id) throws SQLException {
        String sql = """
            SELECT id, nom, nb_terrains, nb_joueurs_par_equipe,
                   date_creation, date_debut, date_fin, statut
            FROM tournoi
            WHERE id = ?
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TournoiMulti(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("nb_terrains"),
                        rs.getInt("nb_joueurs_par_equipe"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getTimestamp("date_debut") != null 
                            ? rs.getTimestamp("date_debut").toLocalDateTime() : null,
                        rs.getTimestamp("date_fin") != null 
                            ? rs.getTimestamp("date_fin").toLocalDateTime() : null,
                        StatutTournoi.valueOf(rs.getString("statut"))
                    );
                }
            }
        }
        return null;
    }

    /**
     * Démarre le tournoi
     */
    public void demarrer(Connection con) throws SQLException {
        if (this.statut != StatutTournoi.EN_PREPARATION) {
            throw new IllegalStateException("Le tournoi n'est pas en préparation");
        }
        this.dateDebut = LocalDateTime.now();
        this.statut = StatutTournoi.EN_COURS;
        this.updateInDB(con);
    }

    /**
     * Termine le tournoi et met à jour le classement global
     */
    public void terminer(Connection con) throws SQLException {
        if (this.statut != StatutTournoi.EN_COURS) {
            throw new IllegalStateException("Le tournoi n'est pas en cours");
        }
        this.dateFin = LocalDateTime.now();
        this.statut = StatutTournoi.TERMINE;
        this.updateInDB(con);
        
        // Mettre à jour le classement global pour tous les joueurs du tournoi
        mettreAJourClassementGlobal(con);
    }

    /**
     * Inscrire un joueur à ce tournoi
     */
    public void inscrireJoueur(Connection con, Joueur joueur) throws SQLException {
        if (this.statut != StatutTournoi.EN_PREPARATION) {
            throw new IllegalStateException("Les inscriptions sont closes");
        }
        
        String sql = "INSERT INTO inscription_tournoi (id_tournoi, id_joueur) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, this.getId());
            ps.setInt(2, joueur.getId());
            ps.executeUpdate();
        }
        this.joueurs.add(joueur);
    }

    /**
     * Récupère tous les joueurs inscrits à ce tournoi
     */
    public List<Joueur> getJoueursInscrits(Connection con) throws SQLException {
        List<Joueur> result = new ArrayList<>();
        String sql = """
            SELECT j.* FROM joueur j
            JOIN inscription_tournoi it ON it.id_joueur = j.id
            WHERE it.id_tournoi = ?
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, this.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Joueur(
                        rs.getInt("id"),
                        rs.getString("surnom"),
                        rs.getString("categorie"),
                        rs.getInt("taillecm"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("sexe"),
                        rs.getDate("date_naissance") != null 
                            ? rs.getDate("date_naissance").toLocalDate() : null
                    ));
                }
            }
        }
        return result;
    }

    // ======================
    // CLASSEMENT GLOBAL
    // ======================

    /**
     * Met à jour le classement global après la fin d'un tournoi
     */
    private void mettreAJourClassementGlobal(Connection con) throws SQLException {
        List<Joueur> joueursInscrits = getJoueursInscrits(con);
        
        for (Joueur j : joueursInscrits) {
            // Compter les statistiques du joueur dans ce tournoi
            int scoreTournoi = calculerScoreJoueurTournoi(con, j.getId());
            int matchsJoues = compterMatchsJoueurTournoi(con, j.getId());
            int victoires = compterVictoiresJoueurTournoi(con, j.getId());
            
            // Mettre à jour ou créer l'entrée dans classement_global
            String sqlCheck = "SELECT id_joueur FROM classement_global WHERE id_joueur = ?";
            boolean existe = false;
            try (PreparedStatement ps = con.prepareStatement(sqlCheck)) {
                ps.setInt(1, j.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    existe = rs.next();
                }
            }
            
            if (existe) {
                String sqlUpdate = """
                    UPDATE classement_global
                    SET nb_tournois_participes = nb_tournois_participes + 1,
                        nb_matchs_joues = nb_matchs_joues + ?,
                        nb_victoires = nb_victoires + ?,
                        score_total = score_total + ?,
                        derniere_mise_a_jour = CURRENT_TIMESTAMP
                    WHERE id_joueur = ?
                    """;
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, matchsJoues);
                    ps.setInt(2, victoires);
                    ps.setInt(3, scoreTournoi);
                    ps.setInt(4, j.getId());
                    ps.executeUpdate();
                }
            } else {
                String sqlInsert = """
                    INSERT INTO classement_global 
                        (id_joueur, nb_tournois_participes, nb_matchs_joues, 
                         nb_victoires, score_total)
                    VALUES (?, 1, ?, ?, ?)
                    """;
                try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                    ps.setInt(1, j.getId());
                    ps.setInt(2, matchsJoues);
                    ps.setInt(3, victoires);
                    ps.setInt(4, scoreTournoi);
                    ps.executeUpdate();
                }
            }
        }
    }

    /**
     * Récupère le classement global de tous les joueurs
     */
    public static List<ClassementGlobalInfo> getClassementGlobal(Connection con) throws SQLException {
        List<ClassementGlobalInfo> result = new ArrayList<>();
        String sql = """
            SELECT j.id, j.surnom, j.nom, j.prenom,
                   cg.nb_tournois_participes, cg.nb_matchs_joues,
                   cg.nb_victoires, cg.score_total
            FROM classement_global cg
            JOIN joueur j ON j.id = cg.id_joueur
            ORDER BY cg.score_total DESC, cg.nb_victoires DESC
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new ClassementGlobalInfo(
                    rs.getInt("id"),
                    rs.getString("surnom"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getInt("nb_tournois_participes"),
                    rs.getInt("nb_matchs_joues"),
                    rs.getInt("nb_victoires"),
                    rs.getInt("score_total")
                ));
            }
        }
        return result;
    }

    // ======================
    // MÉTHODES UTILITAIRES
    // ======================

    private int calculerScoreJoueurTournoi(Connection con, int idJoueur) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(e.score), 0) AS total
            FROM match_joueur mj
            JOIN equipe e ON e.id_match = mj.id_match AND e.numero = mj.numero_equipe
            JOIN matchs m ON m.id = mj.id_match
            JOIN ronde r ON r.id = m.ronde_id
            WHERE mj.id_joueur = ? AND r.id_tournoi = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idJoueur);
            ps.setInt(2, this.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("total");
            }
        }
    }

    private int compterMatchsJoueurTournoi(Connection con, int idJoueur) throws SQLException {
        String sql = """
            SELECT COUNT(DISTINCT mj.id_match)
            FROM match_joueur mj
            JOIN matchs m ON m.id = mj.id_match
            JOIN ronde r ON r.id = m.ronde_id
            WHERE mj.id_joueur = ? AND r.id_tournoi = ?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idJoueur);
            ps.setInt(2, this.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int compterVictoiresJoueurTournoi(Connection con, int idJoueur) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM (
                SELECT m.id
                FROM matchs m
                JOIN ronde r ON r.id = m.ronde_id
                JOIN match_joueur mj ON mj.id_match = m.id
                JOIN equipe e1 ON e1.id_match = m.id AND e1.numero = mj.numero_equipe
                JOIN equipe e2 ON e2.id_match = m.id AND e2.numero != mj.numero_equipe
                WHERE mj.id_joueur = ? AND r.id_tournoi = ? 
                  AND m.statut = 'CLOS' AND e1.score > e2.score
            ) AS victoires
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idJoueur);
            ps.setInt(2, this.getId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ======================
    // GETTERS / SETTERS
    // ======================

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    public int getNbTerrains() { return nbTerrains; }
    public void setNbTerrains(int nbTerrains) { this.nbTerrains = nbTerrains; }
    
    public int getNbJoueursParEquipe() { return nbJoueursParEquipe; }
    public void setNbJoueursParEquipe(int nbJoueursParEquipe) { 
        this.nbJoueursParEquipe = nbJoueursParEquipe; 
    }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public StatutTournoi getStatut() { return statut; }

    public List<Joueur> getJoueurs() { return Collections.unmodifiableList(joueurs); }
    public List<Ronde> getRondes() { return Collections.unmodifiableList(rondes); }

    // ======================
    // CLASSE INTERNE
    // ======================

    public static class ClassementGlobalInfo {
        private int idJoueur;
        private String surnom;
        private String nom;
        private String prenom;
        private int nbTournois;
        private int nbMatchs;
        private int nbVictoires;
        private int scoreTotal;

        public ClassementGlobalInfo(int idJoueur, String surnom, String nom, String prenom,
                                   int nbTournois, int nbMatchs, int nbVictoires, int scoreTotal) {
            this.idJoueur = idJoueur;
            this.surnom = surnom;
            this.nom = nom;
            this.prenom = prenom;
            this.nbTournois = nbTournois;
            this.nbMatchs = nbMatchs;
            this.nbVictoires = nbVictoires;
            this.scoreTotal = scoreTotal;
        }

        // Getters
        public int getIdJoueur() { return idJoueur; }
        public String getSurnom() { return surnom; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public int getNbTournois() { return nbTournois; }
        public int getNbMatchs() { return nbMatchs; }
        public int getNbVictoires() { return nbVictoires; }
        public int getScoreTotal() { return scoreTotal; }
        
        public double getTauxVictoire() {
            return nbMatchs > 0 ? (nbVictoires * 100.0) / nbMatchs : 0.0;
        }
    }
}