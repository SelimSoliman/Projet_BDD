package fr.insa.toto.model;

import com.vaadin.flow.component.sidenav.SideNavItem;
import fr.insa.beuvron.utils.database.ClasseMiroir;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Tournoi extends ClasseMiroir {

    private static final int TAILLE_EQUIPE_FIXE = 2;

  
    

    // --- paramètres généraux ---
    private String nom;
    private int nbTerrains;
    private int nbJoueursParEquipe = TAILLE_EQUIPE_FIXE;

    // --- collections (mémoire) ---
    private final List<Joueur> joueurs = new ArrayList<>();
    private final List<Ronde> rondes = new ArrayList<>();
    private final List<Terrain> terrains = new ArrayList<>();

    // --- Constructeur ---
    public Tournoi(String nom, int nbTerrains) {
        super(); // id = -1
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = TAILLE_EQUIPE_FIXE;
    }
    // Constructeur pour un tournoi récupéré depuis la BD
public Tournoi(int id, String nom, int nbTerrains, int nbJoueursParEquipe) {
    super(id);
    this.nom = nom;
    this.nbTerrains = nbTerrains;
    this.nbJoueursParEquipe = nbJoueursParEquipe;
}
public static Tournoi getInDB(Connection con, int idTournoi) throws SQLException {
    String sql = "SELECT id, nom, nb_terrains, nb_joueurs_par_equipe FROM tournoi WHERE id = ?";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, idTournoi);
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;

            return new Tournoi(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getInt("nb_terrains"),
                rs.getInt("nb_joueurs_par_equipe")
            );
        }
    }
}

 public static Tournoi getTournoiUnique(Connection con) throws SQLException {
    String sql = "select id, nom, nb_terrains, nb_joueurs_par_equipe from tournoi limit 1";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        var rs = ps.executeQuery();
        if (rs.next()) {
            return new Tournoi(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getInt("nb_terrains"),
                rs.getInt("nb_joueurs_par_equipe")
            );
        } else {
            return null;
        }
    }
}
    // =======================
    // Persistance (INSERT)
    // =======================

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
                INSERT INTO tournoi (nom, nb_terrains, nb_joueurs_par_equipe)
                VALUES (?, ?, ?)
                """;
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, this.nom);
        ps.setInt(2, this.nbTerrains);
        ps.setInt(3, this.nbJoueursParEquipe);
        ps.executeUpdate();
        return ps;
    }

    // =======================
    // Persistance (SELECT/UPDATE) : pour la page "Paramètres"
    // =======================

    /** Récupère le tournoi unique (hypothèse du sujet) */
    public Tournoi(int id, String nom, int nbTerrains) {
    super(id); // <-- c'est ça qui fixe l'id
    this.nom = nom;
    this.nbTerrains = nbTerrains;
    this.nbJoueursParEquipe = TAILLE_EQUIPE_FIXE;
}

  public static Tournoi getUnique(Connection con) throws SQLException {
    String sql = "select id, nom, nb_terrains, nb_joueurs_par_equipe from tournoi limit 1";
    try (PreparedStatement pst = con.prepareStatement(sql);
         ResultSet rs = pst.executeQuery()) {

        if (!rs.next()) return null;

        Tournoi t = new Tournoi(
            rs.getInt("id"),
            rs.getString("nom"),
            rs.getInt("nb_terrains")
        );
        t.setNbJoueursParEquipe(rs.getInt("nb_joueurs_par_equipe"));
        return t;
    }
}


    /** Met à jour les paramètres du tournoi */
    public void updateInDB(Connection con) throws SQLException {
        if (this.getId() < 0) {
            throw new IllegalStateException("Tournoi sans id : impossible de faire update");
        }
        String sql = """
                update tournoi
                set nom = ?, nb_terrains = ?, nb_joueurs_par_equipe = ?
                where id = ?
                """;
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, this.nom);
            pst.setInt(2, this.nbTerrains);
            pst.setInt(3, this.nbJoueursParEquipe);
            pst.setInt(4, this.getId());
            pst.executeUpdate();
        }
    }

    // =======================
    // Getters / Setters
    // =======================

    public String getNom() {
        return nom;
    }

    public int getNbTerrains() {
        return nbTerrains;
    }

    public int getNbJoueursParEquipe() {
        return nbJoueursParEquipe;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setNbTerrains(int nbTerrains) {
        this.nbTerrains = nbTerrains;
    }

    public void setNbJoueursParEquipe(int nbJoueursParEquipe) {
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }

    // =======================
    // Gestion joueurs
    // =======================

    public void ajouterJoueur(Joueur j) {
        if (j == null) throw new IllegalArgumentException("Joueur null");
        joueurs.add(j);
    }

    public List<Joueur> getJoueurs() {
        return Collections.unmodifiableList(joueurs);
    }

    // =======================
    // Gestion terrains
    // =======================

    public void ajouterTerrain(Terrain t) {
        if (t == null) throw new IllegalArgumentException("Terrain null");
        terrains.add(t);
    }

    public List<Terrain> getTerrains() {
        return Collections.unmodifiableList(terrains);
    }

    // =======================
    // Gestion rondes
    // =======================

    public Ronde nouvelleRonde() {
        if (!rondes.isEmpty()) {
            Ronde derniere = rondes.get(rondes.size() - 1);
            if (!derniere.isClose()) {
                throw new IllegalStateException(
                        "Impossible de creer une nouvelle ronde : la ronde "
                                + derniere.getNumero() + " n'est pas encore close.");
            }
        }
        Ronde r = new Ronde(this.getId(), rondes.size() + 1);
        rondes.add(r);
        return r;
    }

    public List<Ronde> getRondes() {
        return Collections.unmodifiableList(rondes);
    }

    // =======================
    // Classement joueurs (squelette)
    // =======================

   

    // =======================
    // Création matchs pour une ronde
    // =======================

    /** Version pratique : utilise la taille d'équipe du tournoi */
    public void creerMatchsPourRonde(Ronde r, Connection con) throws SQLException {
        creerMatchsPourRonde(r, this.nbJoueursParEquipe, con);
    }

    public void creerMatchsPourRonde(Ronde r, int tailleEquipe, Connection con) throws SQLException {
    if (r == null) throw new IllegalArgumentException("Ronde null");
    if (tailleEquipe <= 0) throw new IllegalArgumentException("tailleEquipe invalide");

    List<Joueur> tous = new ArrayList<>(this.joueurs);

    int joueursParMatch = 2 * tailleEquipe;
    int nbMatchsPossible = tous.size() / joueursParMatch;

    if (nbMatchsPossible == 0) {
        throw new IllegalStateException("Pas assez de joueurs pour faire un match");
    }
    if (terrains.isEmpty()) {
        throw new IllegalStateException("Aucun terrain disponible");
    }

    int nbMatchs = Math.min(nbMatchsPossible, terrains.size());
    Collections.shuffle(tous);

    for (int i = 0; i < nbMatchs; i++) {
    Terrain terrain = terrains.get(i);

    // 1) créer le match (neuf -> id = -1)
    Match m = new Match(r, terrain);

    // 2) sauver le match UNE SEULE FOIS (pour obtenir m.getId())
    m.saveInDB(con);

    // 3) remplir les équipes en mémoire
    Equipe e1 = m.getEquipe1();
    Equipe e2 = m.getEquipe2();

    for (int j = 0; j < tailleEquipe; j++) {
        Joueur j1 = tous.remove(0);
        Joueur j2 = tous.remove(0);
        e1.ajouterJoueur(j1);
        e2.ajouterJoueur(j2);
    }

    // 4) sauver équipes + match_joueur (UNE SEULE méthode)
    m.saveEquipesEtJoueurs(con);

    // 5) ajouter le match à la ronde
    r.ajouterMatch(m);
}

}

public void clearTerrains() {
    this.terrains.clear();
}

public void clearJoueurs() {
    this.joueurs.clear();
}

    public int computeScore(Joueur j, Connection con) throws SQLException {
    String sql = """
        SELECT COALESCE(SUM(e.score), 0) AS total
        FROM match_joueur mj
        JOIN equipe e
          ON e.id_match = mj.id_match
         AND e.numero = mj.numero_equipe
        WHERE mj.id_joueur = ?
        """;
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, j.getId());
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("total");
        }
    }
}

public List<Joueur> classementJoueurs(Connection con) {
    List<Joueur> copie = new ArrayList<>(this.joueurs);

    copie.sort(new Comparator<Joueur>() {
        @Override
        public int compare(Joueur a, Joueur b) {
            try {
                return Integer.compare(computeScore(b, con), computeScore(a, con)); // desc

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    });

    return copie;
}




public int genererMatchsPourRonde(Ronde r, Connection con) throws SQLException {

    // 1) récupérer tous les joueurs depuis la liste déjà chargée dans Tournoi
    List<Joueur> pool = new ArrayList<>(this.joueurs);
    Collections.shuffle(pool);

    int tailleEquipe = this.nbJoueursParEquipe;
    int joueursParMatch = 2 * tailleEquipe;

    // 2) combien de matchs possibles ?
    int nbMatchsPossible = pool.size() / joueursParMatch;
    if (nbMatchsPossible == 0) {
        throw new IllegalStateException("Pas assez de joueurs pour créer un match");
    }

    // 3) on est limité par les terrains dispo (dans ta liste terrains)
    int nbMatchs = Math.min(nbMatchsPossible, this.terrains.size());

    String sqlInsertMatch = """
        insert into matchs (ronde_id, terrain_id, score_e1, score_e2, statut)
        values (?, ?, 0, 0, 'EN_COURS')
        """;

    String sqlInsertMJ = """
        insert into match_joueur (id_match, id_joueur, numero_equipe)
        values (?, ?, ?)
        """;

    try (
        PreparedStatement pstMatch = con.prepareStatement(sqlInsertMatch, Statement.RETURN_GENERATED_KEYS);
        PreparedStatement pstMJ = con.prepareStatement(sqlInsertMJ)
    ) {
        for (int i = 0; i < nbMatchs; i++) {
            Terrain terrain = this.terrains.get(i);

            // 4) créer le match
            pstMatch.setInt(1, r.getId());
            pstMatch.setInt(2, terrain.getId());
            pstMatch.executeUpdate();

            var rsKeys = pstMatch.getGeneratedKeys();
            rsKeys.next();
            int idMatch = rsKeys.getInt(1);

            // 5) remplir équipe 1
            for (int k = 0; k < tailleEquipe; k++) {
                Joueur j = pool.remove(0);
                pstMJ.setInt(1, idMatch);
                pstMJ.setInt(2, j.getId());
                pstMJ.setInt(3, 1);
                pstMJ.executeUpdate();
            }

            // 6) remplir équipe 2
            for (int k = 0; k < tailleEquipe; k++) {
                Joueur j = pool.remove(0);
                pstMJ.setInt(1, idMatch);
                pstMJ.setInt(2, j.getId());
                pstMJ.setInt(3, 2);
                pstMJ.executeUpdate();
            }
        }
    }

    // nb de matchs créés
    return nbMatchs;
}


}
