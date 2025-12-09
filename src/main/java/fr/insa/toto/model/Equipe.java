/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.toto.model;

/**
 *
 * @author win
 */
public class Equipe {
   
    private static final int TAILLE_REQUISE = 2;  
    
    private int id;
    private int matchId;
    private int numeroEquipe;  
    private int score;
    private List<Joueur> joueurs;  
    
    public Equipe() {
        this.joueurs = new ArrayList<>();
        this.score = 0;
    }
    
    // Getter pour la taille actuelle
    public int getTailleActuelle() {
        return joueurs.size();
    }
    
    // Vérifier si l'équipe est complète
    public boolean estComplete() {
        return joueurs.size() == TAILLE_REQUISE;
    }
    
    // Vérifier si l'équipe est valide (pour clôturer un match)
    public boolean estValide() {
        return joueurs.size() == TAILLE_REQUISE;
    }
    
    // Ajouter un joueur avec validation
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
    
    // Retirer un joueur
    public void retirerJoueur(Joueur joueur) {
        if (joueurs.remove(joueur)) {
            System.out.println("Joueur retiré de l'équipe");
        } else {
            throw new IllegalArgumentException("Ce joueur n'est pas dans l'équipe");
        }
    }
    
    // Obtenir la liste des joueurs
    public List<Joueur> getJoueurs() {
        return new ArrayList<>(joueurs);  // Retourner une copie (immutabilité)
    }
    
    // Getters et setters standards
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getMatchId() {
        return matchId;
    }
    
    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }
    
    public int getNumeroEquipe() {
        return numeroEquipe;
    }
    
    public void setNumeroEquipe(int numeroEquipe) {
        if (numeroEquipe != 1 && numeroEquipe != 2) {
            throw new IllegalArgumentException("Le numéro d'équipe doit être 1 ou 2");
        }
        this.numeroEquipe = numeroEquipe;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        if (score < 0) {
            throw new IllegalArgumentException("Le score ne peut pas être négatif");
        }
        this.score = score;
    }
    
    public static int getTailleRequise() {
        return TAILLE_REQUISE;
    }
    
    @Override
    public String toString() {
        return "Équipe " + numeroEquipe + " (" + getTailleActuelle() + "/" + TAILLE_REQUISE + " joueurs) - Score: " + score;
    }
}


