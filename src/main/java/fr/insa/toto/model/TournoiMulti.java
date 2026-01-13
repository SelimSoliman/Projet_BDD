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

/**
 * Classe représentant un tournoi multi-joueurs.
 * Hérite de ClasseMiroir pour la gestion de l'ID et de la sauvegarde en BDD.
 */
public class TournoiMulti extends ClasseMiroir {


public List<Joueur> getJoueursInscrits(Connection con) throws SQLException {
    List<Joueur> res = new ArrayList<>();

    String sql = """
        SELECT j.id, j.surnom, j.categorie, j.taillecm, j.nom, j.prenom, j.sexe, j.date_naissance
        FROM joueur j
        JOIN inscription_tournoi it ON it.id_joueur = j.id
        WHERE it.id_tournoi = ?
        """;

    try (PreparedStatement pst = con.prepareStatement(sql)) {
        pst.setInt(1, this.getId());
        try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(Joueur.fromResultSet(rs));
            }
        }
    }

    return res;
}

    /**
     * Inscrit automatiquement tous les joueurs de la base au tournoi.
     * Utile pour la création rapide de rondes.
     */
    public void inscrireTousLesJoueurs(Connection con) throws SQLException {
        String sqlJoueurs = "SELECT id FROM joueur";
        String sqlInsert = "INSERT IGNORE INTO inscription_tournoi (id_tournoi, id_joueur) VALUES (?, ?)";
        
        try (PreparedStatement psSelect = con.prepareStatement(sqlJoueurs);
             PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
            
            try (ResultSet rs = psSelect.executeQuery()) {
                while (rs.next()) {
                    int idJoueur = rs.getInt("id");
                    psInsert.setInt(1, this.getId());
                    psInsert.setInt(2, idJoueur);
                    psInsert.executeUpdate();
                }
            }
        }
    }

    public enum StatutTournoi {
        A_VENIR,
        EN_COURS,
        TERMINE
    }

    private String nom;
    private int nbTerrains;
    private int nbJoueursParEquipe;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private StatutTournoi statut;

   

    // ========================================
    // CONSTRUCTEURS
    // ========================================

    /**
     * Constructeur pour un nouveau tournoi EN MÉMOIRE (pas encore en BDD).
     * L'ID sera généré lors du saveInDB().
     */
    public TournoiMulti(String nom, int nbTerrains, int nbJoueursParEquipe) {
        super(); // Appelle ClasseMiroir() qui met id = -1
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
        this.dateCreation = LocalDateTime.now();
        this.dateDebut = null;
        this.dateFin = null;
        this.statut = StatutTournoi.A_VENIR;
    }

    /**
     * Constructeur pour un tournoi CHARGÉ DEPUIS LA BDD.
     * Utilisé par les méthodes de récupération (getById, tousLesTournois, etc.)
     */
    public TournoiMulti(int id, String nom, int nbTerrains, int nbJoueursParEquipe,
                        LocalDateTime dateCreation, LocalDateTime dateDebut,
                        LocalDateTime dateFin, StatutTournoi statut) {
        super(id); // Appelle ClasseMiroir(int id)
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
        this.dateCreation = dateCreation;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = (statut == null) ? StatutTournoi.A_VENIR : statut;
    }

    /**
     * Constructeur SIMPLIFIÉ pour compatibilité avec l'ancien code.
     * (sans dates et statut)
     */
    public TournoiMulti(int id, String nom, int nbTerrains, int nbJoueursParEquipe) {
        this(id, nom, nbTerrains, nbJoueursParEquipe, 
             LocalDateTime.now(), null, null, StatutTournoi.A_VENIR);
    }

    // ========================================
    // SAUVEGARDE EN BDD
    // ========================================

    /**
     * ⚠️ NE PAS redéfinir saveInDB() car elle est FINAL dans ClasseMiroir.
     * À la place, on redéfinit saveSansId() qui est appelée par saveInDB().
     */
    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        // ✅ CORRECTION : Utiliser getId() au lieu de this.id
        if (this.getId() == -1) {
            // Insertion d'un nouveau tournoi
            PreparedStatement pst = con.prepareStatement(
                "INSERT INTO tournoi (nom, nb_terrains, nb_joueurs_par_equipe) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            pst.setString(1, this.nom);
            pst.setInt(2, this.nbTerrains);
            pst.setInt(3, this.nbJoueursParEquipe);
            pst.executeUpdate();
            return pst;
        } else {
            // Mise à jour d'un tournoi existant
            PreparedStatement pst = con.prepareStatement(
                "UPDATE tournoi SET nom = ?, nb_terrains = ?, nb_joueurs_par_equipe = ? WHERE id = ?"
            );
            pst.setString(1, this.nom);
            pst.setInt(2, this.nbTerrains);
            pst.setInt(3, this.nbJoueursParEquipe);
            pst.setInt(4, this.getId()); // ✅ Utiliser getId()
            pst.executeUpdate();
            return pst;
        }
    }

    // ========================================
    // MÉTHODES DE RÉCUPÉRATION
    // ========================================

    /**
     * Récupère tous les tournois de la base de données.
     */
    public static List<TournoiMulti> tousLesTournois(Connection con) throws SQLException {
        List<TournoiMulti> res = new ArrayList<>();
        
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT id, nom, nb_terrains, nb_joueurs_par_equipe FROM tournoi ORDER BY id DESC")) {
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new TournoiMulti(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("nb_terrains"),
                        rs.getInt("nb_joueurs_par_equipe")
                    ));
                }
            }
        }
        
        return res;
    }

    /**
     * Récupère un tournoi par son ID.
     */
    public static TournoiMulti getById(Connection con, int id) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT id, nom, nb_terrains, nb_joueurs_par_equipe FROM tournoi WHERE id = ?")) {
            
            pst.setInt(1, id);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new TournoiMulti(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("nb_terrains"),
                        rs.getInt("nb_joueurs_par_equipe")
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * Supprime le tournoi de la base de données.
     * ATTENTION : Supprime aussi toutes les données associées.
     */
    public void deleteFromDB(Connection con) throws SQLException {
        // ✅ CORRECTION : Utiliser getId() au lieu de this.id
        if (this.getId() == -1) {
            throw new IllegalStateException("Impossible de supprimer un tournoi non sauvegardé");
        }

        // Supprimer dans l'ordre inverse des dépendances
        try (PreparedStatement pst = con.prepareStatement(
                "DELETE FROM match_joueur WHERE id_match IN " +
                "(SELECT id FROM matchs WHERE ronde_id IN " +
                "(SELECT id FROM ronde WHERE id_tournoi = ?))")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }

        try (PreparedStatement pst = con.prepareStatement(
                "DELETE FROM matchs WHERE ronde_id IN " +
                "(SELECT id FROM ronde WHERE id_tournoi = ?)")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }

        try (PreparedStatement pst = con.prepareStatement(
                "DELETE FROM equipe WHERE id_match IN " +
                "(SELECT id FROM matchs WHERE ronde_id IN " +
                "(SELECT id FROM ronde WHERE id_tournoi = ?))")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }

        try (PreparedStatement pst = con.prepareStatement(
                "DELETE FROM ronde WHERE id_tournoi = ?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }

        try (PreparedStatement pst = con.prepareStatement(
                "DELETE FROM inscription_tournoi WHERE id_tournoi = ?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }

        try (PreparedStatement pst = con.prepareStatement(
                "DELETE FROM tournoi WHERE id = ?")) {
            pst.setInt(1, this.getId());
            pst.executeUpdate();
        }
    }

    // ========================================
    // CLASSEMENT GLOBAL
    // ========================================

    /**
     * Récupère le classement global de tous les joueurs d'un tournoi.
     */
    public List<ClassementGlobalInfo> getClassementGlobal(Connection con) throws SQLException {
        List<ClassementGlobalInfo> classement = new ArrayList<>();

        String sql = 
            "SELECT j.id AS idJoueur, j.surnom, j.nom, j.prenom, " +
            "       COUNT(DISTINCT r.id) AS nbTournois, " +
            "       COUNT(DISTINCT m.id) AS nbMatchs, " +
            "       SUM(CASE WHEN e.score > e2.score THEN 1 ELSE 0 END) AS nbVictoires, " +
            "       SUM(e.score) AS scoreTotal " +
            "FROM joueur j " +
            "JOIN match_joueur mj ON j.id = mj.id_joueur " +
            "JOIN equipe e ON mj.id_equipe = e.id " +
            "JOIN matchs m ON e.id_match = m.id " +
            "JOIN ronde r ON m.ronde_id = r.id " +
            "LEFT JOIN equipe e2 ON m.id = e2.id_match AND e2.numero != e.numero " +
            "WHERE r.id_tournoi = ? " +
            "GROUP BY j.id, j.surnom, j.nom, j.prenom " +
            "ORDER BY scoreTotal DESC, nbVictoires DESC";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, this.getId());
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    classement.add(new ClassementGlobalInfo(
                        rs.getInt("idJoueur"),
                        rs.getString("surnom"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getInt("nbTournois"),
                        rs.getInt("nbMatchs"),
                        rs.getInt("nbVictoires"),
                        rs.getInt("scoreTotal")
                    ));
                }
            }
        }

        return classement;
    }

    // ========================================
    // GETTERS / SETTERS
    // ========================================

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getNbTerrains() {
        return nbTerrains;
    }

    public void setNbTerrains(int nbTerrains) {
        this.nbTerrains = nbTerrains;
    }

    public int getNbJoueursParEquipe() {
        return nbJoueursParEquipe;
    }

    public void setNbJoueursParEquipe(int nbJoueursParEquipe) {
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public StatutTournoi getStatut() {
        return statut;
    }

    public void setStatut(StatutTournoi statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "TournoiMulti{" +
                "id=" + getId() +
                ", nom='" + nom + '\'' +
                ", terrains=" + nbTerrains +
                ", joueurs/equipe=" + nbJoueursParEquipe +
                ", statut=" + statut +
                '}';
    }
 


    // ========================================
    // CLASSE INTERNE (C'EST NORMAL !)
    // ========================================

    /**
     * ✅ CLASSE INTERNE STATIQUE : C'est une bonne pratique !
     * Elle sert à transporter les données de classement.
     */
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

        @Override
        public String toString() {
            return surnom + " (" + nom + " " + prenom + ") - " +
                   "Matchs: " + nbMatchs + ", Victoires: " + nbVictoires +
                   ", Score: " + scoreTotal;
        }
    }
}