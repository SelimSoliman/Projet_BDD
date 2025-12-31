package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;

import java.sql.Connection;
import java.sql.PreparedStatement;
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

    // --- collections ---
    private List<Joueur> joueurs = new ArrayList<>();
    private List<Ronde> rondes = new ArrayList<>();
    private List<Terrain> terrains = new ArrayList<>();

    // --- Constructeur pour un nouveau tournoi ---
    public Tournoi(String nom, int nbTerrains) {
        super(); // id = -1
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = TAILLE_EQUIPE_FIXE;
    }

    // --- Persistance ---

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO tournoi (nom, nb_terrains, nb_joueurs_par_equipe)
            VALUES (?, ?, ?)
            """;
        PreparedStatement ps =
            con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, this.nom);
        ps.setInt(2, this.nbTerrains);
        ps.setInt(3, this.nbJoueursParEquipe);

        ps.executeUpdate();   // exécute l'INSERT

        return ps;
    }

    // --- Getters de configuration ---

    public String getNom() { return nom; }

    public int getNbTerrains() { return nbTerrains; }

    public int getNbJoueursParEquipe() { return nbJoueursParEquipe; }

    // --- Gestion des joueurs ---

    public void ajouterJoueur(Joueur j) {
        if (j == null) {
            throw new IllegalArgumentException("Joueur null");
        }
        joueurs.add(j);
    }

    public List<Joueur> getJoueurs() {
        return Collections.unmodifiableList(joueurs);
    }

    // --- Gestion des terrains (extension simple) ---

   public void ajouterTerrain(Terrain t) {
    if (t == null) {
        throw new IllegalArgumentException("Terrain null");
    }
    terrains.add(t);
}


    public List<Terrain> getTerrains() {
        return Collections.unmodifiableList(terrains);
    }

    // --- Gestion des rondes ---

    /**
     * Crée la ronde suivante du tournoi (1, 2, 3, ...).
     * Le sujet impose que toutes les rondes précédentes soient closes.
     */
   public Ronde nouvelleRonde() {
    if (!rondes.isEmpty()) {
        Ronde derniere = rondes.get(rondes.size() - 1);
        if (!derniere.isClose()) {
            throw new IllegalStateException(
                "Impossible de creer une nouvelle ronde : la ronde "
                + derniere.getNumero() + " n'est pas encore close.");
        }
    }
    Ronde r = new Ronde(this, rondes.size() + 1);
    rondes.add(r);
    return r;
}

    public List<Ronde> getRondes() {
        return Collections.unmodifiableList(rondes);
    }

    // --- Classement des joueurs (simplifié) ---

    /**
     * Classe les joueurs par score total sur l'ensemble du tournoi.
     * Le calcul précis dépendra de la façon dont tu stockes les scores par joueur
     * (via Match_Joueur). Ici on ne fait que prévoir le squelette.
     */
    public List<Joueur> classementJoueurs() {
        List<Joueur> copie = new ArrayList<>(joueurs);
        // À implémenter : computeScore(j) doit utiliser Match_Joueur
        copie.sort(Comparator.comparingInt(this::computeScore).reversed());
        return copie;
    }

    // Méthode à compléter plus tard avec la vraie logique
    private int computeScore(Joueur j) {
        // TODO : sommer les scores obtenus dans tous les matchs
        return 0;
    }
}
