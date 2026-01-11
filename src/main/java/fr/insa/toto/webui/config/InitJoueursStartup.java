package fr.insa.toto.webui.config;

import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Initialisation automatique de 10 joueurs au demarrage de l'application
 */
@Component
public class InitJoueursStartup implements CommandLineRunner {

    @Override
    public void run(String... args) {
        try (Connection con = ConnectionPool.getConnection()) {
            
            // Verifier si des joueurs existent dejà
            List<Joueur> joueursExistants = Joueur.tousLesJoueurs(con);
            
            if (joueursExistants.size() >= 10) {
                System.out.println("✅ " + joueursExistants.size() + " joueur(s) dejà present(s) en base.");
                return;
            }
            
            System.out.println("🎾 Initialisation de 10 joueurs par defaut...");
            
            // Liste de 10 joueurs varies
            String[][] joueurs = {
                {"Alice", "Martin", "Alice", "Intermediaire", "170", "F"},
                {"Bob", "Dupont", "Bob", "Avance", "180", "M"},
                {"Clara", "Bernard", "Clara", "Debutant", "165", "F"},
                {"David", "Petit", "David", "Expert", "185", "M"},
                {"Emma", "Durand", "Emma", "Intermediaire", "168", "F"},
                {"Florian", "Moreau", "Flo", "Avance", "178", "M"},
                {"Gabrielle", "Simon", "Gaby", "Debutant", "162", "F"},
                {"Hugo", "Laurent", "Hugo", "Expert", "182", "M"},
                {"Inès", "Lefebvre", "Inès", "Intermediaire", "166", "F"},
                {"Jules", "Roux", "Jules", "Avance", "175", "M"}
            };
            
            int joueursAjoutes = 0;
            
            for (String[] j : joueurs) {
                try {
                    String nom = j[0];
                    String prenom = j[1];
                    String surnom = j[2];
                    String categorie = j[3];
                    int taille = Integer.parseInt(j[4]);
                    String sexe = j[5];
                    
                    // Verifier si le joueur existe dejà
                    boolean existe = joueursExistants.stream()
                        .anyMatch(joueur -> joueur.getSurnom().equalsIgnoreCase(surnom));
                    
                    if (!existe) {
                        Joueur joueur = new Joueur(
                            surnom,
                            categorie,
                            taille,
                            nom,
                            prenom,
                            sexe,
                            LocalDate.now()
                        );
                        
                        joueur.saveInDB(con);
                        joueursAjoutes++;
                        System.out.println("   ✅ Joueur cree : " + surnom + " (" + categorie + ")");
                    }
                    
                } catch (Exception e) {
                    System.err.println("   ❌ Erreur lors de la creation du joueur : " + e.getMessage());
                }
            }
            
            if (joueursAjoutes > 0) {
                System.out.println("🎉 " + joueursAjoutes + " joueur(s) cree(s) avec succès !");
            } else {
                System.out.println("ℹ️ Tous les joueurs etaient dejà presents.");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur d'initialisation des joueurs : " + e.getMessage());
            e.printStackTrace();
        }
    }
}