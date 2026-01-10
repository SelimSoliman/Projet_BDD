package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;

public class Equipe extends ClasseMiroir {
    public void setScore(int score) {
    if (score < 0) throw new IllegalArgumentException("score negatif");
    this.score = score; // adapte au nom exact de ton attribut
}


    private static final int TAILLE_REQUISE = 2;

    private Match match;
    private int numero;   // 1 ou 2
    private int score;
    private List<Joueur> joueurs;

    public Equipe(Match match, int numero) {
        super(); // id = -1
        this.match = match;
        this.numero = numero;
        this.joueurs = new ArrayList<>();
        this.score = 0;
    }
/// constructeur pour equipe standalone (sans match associe)
public Equipe() {
    super();  // id = -1
    this.match = null;
    this.numero = 0;
    this.joueurs = new ArrayList<>();
    this.score = 0;
}

// constructeur avec id (pour chargement depuis BD)
public Equipe(int id) {
    super(id);  // appelle ClasseMiroir(int id)
    this.match = null;
    this.numero = 0;
    this.joueurs = new ArrayList<>();
    this.score = 0;
}


    // --------- GETTERS / SETTERS ---------

    public Match getMatch() { return match; }

    public int getNumero() { return numero; }

    public int getTailleActuelle() {
        return joueurs.size();
    }

    public boolean estComplete() {
        return joueurs.size() == TAILLE_REQUISE;
    }

    public boolean estValide() {
        return joueurs.size() == TAILLE_REQUISE;
    }

    public List<Joueur> getJoueurs() {
        return new ArrayList<>(joueurs);
    }

    public int getScoreTotal() { return score; }

    public void ajouterScore(int score) {
        this.score += score;
    }

    // --------- GESTION DES JOUEURS ---------

    public void ajouterJoueur(Joueur joueur) {
        if (joueurs.size() >= TAILLE_REQUISE) {
            throw new IllegalStateException(
                "Impossible d'ajouter un joueur. L'équipe est déjà complète avec "
                + TAILLE_REQUISE + " joueurs."
            );
        }
        if (joueur == null) {
            throw new IllegalArgumentException("Le joueur ne peut pas être null");
        }
        if (joueurs.contains(joueur)) {
            throw new IllegalArgumentException("Ce joueur est déjà dans l'équipe");
        }
        joueurs.add(joueur);
    }

    public void retirerJoueur(Joueur joueur) {
        if (!joueurs.remove(joueur)) {
            throw new IllegalArgumentException("Ce joueur n'est pas dans l'équipe");
        }
    }

    // --------- PERSISTENCE EQUIPE ---------

    @Override
protected PreparedStatement saveSansId(Connection con) throws SQLException {
    if (this.match == null || this.match.getId() == -1) {
    throw new IllegalStateException("Equipe sans match sauvegarde (id_match invalide)");
}

    String sql = """
        INSERT INTO equipe (id_match, numero, score)
        VALUES (?, ?, ?)
        """;
    PreparedStatement ps =
        con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

    // si match est null, on met NULL en base
    ps.setInt(1, this.match.getId());

    
    ps.setInt(2, this.numero);
    ps.setInt(3, this.score);

    ps.executeUpdate();   // exécute l'INSERT

    return ps;
}
public static List<Equipe> toutesLesEquipesDeLaRonde(Connection con, int idRonde)
        throws SQLException {
    // SELECT * FROM equipe WHERE ronde_id = ?
        return null;
    // SELECT * FROM equipe WHERE ronde_id = ?
}



    /**
     * Sauvegarde l'association joueurs <-> match/équipe
     * dans la table Match_Joueur (id_match, id_joueur, numero_equipe).
     */
    public void saveJoueursDansEquipe(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new ClasseMiroir.EntiteNonSauvegardee();
        }
        String sql = """
            INSERT INTO match_joueur (id_match, id_joueur, numero_equipe)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Joueur j : joueurs) {
                ps.setInt(1, this.match.getId());
                ps.setInt(2, j.getId());
                ps.setInt(3, this.numero);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
/**
 * Recupere toutes les equipes en base.
 */
/**
 * Recupere toutes les equipes en base.
 */
public static List<Equipe> toutesLesEquipes(Connection con) throws SQLException {
    String sql = "SELECT id, id_match, numero, score FROM equipe";
    List<Equipe> equipes = new ArrayList<>();
    
    try (PreparedStatement ps = con.prepareStatement(sql);
         java.sql.ResultSet rs = ps.executeQuery()) {  // utilise java.sql.ResultSet
        
        while (rs.next()) {
            int id = rs.getInt("id");
            int idMatch = rs.getInt("id_match");
            int numero = rs.getInt("numero");
            int score = rs.getInt("score");
            
            // on cree une equipe standalone (sans charger le Match complet)
            Equipe e = new Equipe(id);  // utilise le nouveau constructeur

            e.numero = numero;
            e.score = score;
            
            // charger les joueurs de cette equipe
            String sqlJoueurs = """
                SELECT j.* FROM joueur j
                JOIN match_joueur mj ON j.id = mj.id_joueur
                WHERE mj.id_match = ? AND mj.numero_equipe = ?
                """;
            try (PreparedStatement psJ = con.prepareStatement(sqlJoueurs);) {
                psJ.setInt(1, idMatch);
                psJ.setInt(2, numero);
                try (java.sql.ResultSet rsJ = psJ.executeQuery()) {
                    while (rsJ.next()) {
                        Joueur j = new Joueur(
                            rsJ.getInt("id"),
                            rsJ.getString("surnom"),
                            rsJ.getString("categorie"),
                            rsJ.getInt("taille_cm"),
                            rsJ.getString("nom"),
                            rsJ.getString("prenom"),
                            rsJ.getString("sexe"),
                            rsJ.getDate("date_naissance") != null 
                                ? rsJ.getDate("date_naissance").toLocalDate() 
                                : null
                        );
                        e.joueurs.add(j);
                    }
                }
            }
            
            equipes.add(e);
        }
    }
    return equipes;
}


/**
 * Supprime une equipe et ses associations joueurs.
 */
public static void supprimer(Connection con, int id) throws SQLException {
    // d'abord recuperer l'id_match et le numero pour supprimer les liens
    String sqlInfo = "SELECT id_match, numero FROM equipe WHERE id = ?";
    int idMatch = -1;
    int numero = -1;
    
    try (PreparedStatement ps = con.prepareStatement(sqlInfo)) {
        ps.setInt(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                idMatch = rs.getInt("id_match");
                numero = rs.getInt("numero");
            }
        }
    }
    
    // supprimer les liens dans match_joueur
    if (idMatch != -1) {
        String sqlJoueurs = "DELETE FROM match_joueur WHERE id_match = ? AND numero_equipe = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlJoueurs)) {
            ps.setInt(1, idMatch);
            ps.setInt(2, numero);
            ps.executeUpdate();
        }
    }
    
    // supprimer l'equipe
    String sqlEquipe = "DELETE FROM equipe WHERE id = ?";
    try (PreparedStatement ps = con.prepareStatement(sqlEquipe)) {
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}



    // --------- DIVERS ---------

    @Override
    public String toString() {
        return "Équipe " + numero + " (" + getTailleActuelle() + "/" +
               TAILLE_REQUISE + " joueurs) - Score: " + score;
    }
}
