import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.util.List;
import java.util.ArrayList;
import fr.insa.toto.model.Joueur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Equipe extends ClasseMiroir {
   
    private static final int TAILLE_REQUISE = 2;  
    
    
    private int matchId;
    private int nom;  
    private int score;
    private List<Joueur> joueurs; 
    
    
    public Equipe() {
        this.joueurs = new ArrayList<>();
        this.score = 0;
    }
    
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
        if (joueurs.remove(joueur)) {  
            System.out.println("Joueur retiré de l'équipe");
        } else {
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
    String sql = "INSERT INTO Equipe (match_id, nom, score) VALUES (?, ?, ?)";
    PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

    ps.setInt(1, this.matchId);
    ps.setString(2, String.valueOf(this.nom)); // ou directement this.nom si tu passes nom en String
    ps.setInt(3, this.score);

    // NE PAS exécuter ici, on retourne juste le Statement
    return ps;
}

    
    @Override
    public String toString() {
        return "Équipe " + nom + " (" + getTailleActuelle() + "/" + TAILLE_REQUISE + " joueurs) - Score: " + score;
    }
}
