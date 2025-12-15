package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;

public class Equipe extends ClasseMiroir {
   
    private static final int TAILLE_REQUISE = 2;  
    
    private int id;
    private Match match;
    private int numero;   // 1 ou 2
    private int score;
    private List<Joueur> joueurs; 
    
    public Equipe(Match match, int numero) {
        this.match = match;
        this.numero = numero;
        this.joueurs = new ArrayList<>();
        this.score = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
    
    public List<Joueur> getJoueurs() {
        return new ArrayList<>(joueurs);  
    }
    
    public int getScoreTotal() { return score; }

    public void ajouterScore(int score) {
        this.score += score;
    }

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO Equipe (id_match, numero, score)
            VALUES (?, ?, ?)
        """;
        PreparedStatement ps =
            con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, this.match.getId());
        ps.setInt(2, this.numero);
        ps.setInt(3, this.score);

        return ps;
    }

    @Override
    public String toString() {
        return "Équipe " + numero + " (" + getTailleActuelle() + "/" +
               TAILLE_REQUISE + " joueurs) - Score: " + score;
    }
}
