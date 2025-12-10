import java.util.List;
import java.util.ArrayList;
import fr.insa.toto.model.Joueur;

public class Equipe {
   
    private static final int TAILLE_REQUISE = 2;  
    
    private int id;
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
    public String toString() {
        return "Équipe " + nom + " (" + getTailleActuelle() + "/" + TAILLE_REQUISE + " joueurs) - Score: " + score;
    }
}
