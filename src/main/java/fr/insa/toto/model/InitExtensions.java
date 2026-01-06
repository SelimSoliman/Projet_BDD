package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Script d'initialisation pour créer le schéma complet
 * avec toutes les extensions
 */
public class InitExtensions {

    /**
     * Crée le schéma complet avec toutes les tables
     */
    public static void creerSchemaComplet(Connection con) throws SQLException {
        System.out.println("=== Création du schéma de base ===");
        GestionBDD.creeSchema(con);

        System.out.println("=== Extension 2 : Multi-tournoi ===");
        TournoiMulti.creerSchemaMultiTournoi(con);

        System.out.println("=== Extension 3 : Terrains avec plan ===");
        TerrainAvecPlan.ajouterColonnesPlan(con);

        System.out.println("=== Extensions 4-5 : Types de jeu ===");
        TypeJeu.creerSchemTypeJeu(con);

        System.out.println("=== Extension 6 : Templates ===");
        TemplateTournoi.creerSchemaTemplate(con);

        System.out.println("✓ Schéma complet créé");
    }

    /**
     * Crée des données de test pour toutes les extensions
     */
    public static void creerDonneesTest(Connection con) throws SQLException {
        System.out.println("\n=== Création des données de test ===");

        // Utilisateurs
        System.out.println("- Utilisateurs...");
        Utilisateur admin = new Utilisateur("admin", "admin", 1);
        admin.saveInDB(con);
        Utilisateur user1 = new Utilisateur("alice", "pass", 2);
        user1.saveInDB(con);
        Utilisateur user2 = new Utilisateur("bob", "pass", 2);
        user2.saveInDB(con);

        // Joueurs
        System.out.println("- Joueurs...");
        List<Joueur> joueurs = List.of(
            new Joueur("alice", "Avancé", 170, "Durand", "Alice", "F", 
                      LocalDate.of(1995, 3, 15)),
            new Joueur("bob", "Débutant", 180, "Martin", "Bob", "M", 
                      LocalDate.of(1998, 7, 22)),
            new Joueur("charlie", "Intermédiaire", 175, "Dubois", "Charlie", "M", 
                      LocalDate.of(1996, 11, 8)),
            new Joueur("diana", "Avancé", 165, "Petit", "Diana", "F", 
                      LocalDate.of(1997, 2, 14)),
            new Joueur("eve", "Expert", 172, "Rousseau", "Eve", "F", 
                      LocalDate.of(1994, 9, 30)),
            new Joueur("frank", "Intermédiaire", 182, "Moreau", "Frank", "M", 
                      LocalDate.of(1999, 5, 18))
        );
        for (Joueur j : joueurs) {
            j.saveInDB(con);
        }

        // Terrains avec plan (Extension 3)
        System.out.println("- Terrains avec plan...");
        List<TerrainAvecPlan> terrains = List.of(
            new TerrainAvecPlan("Court Central", "Grand terrain principal avec gradins"),
            new TerrainAvecPlan("Court 2", "Terrain annexe côté Est"),
            new TerrainAvecPlan("Court 3", "Terrain d'entraînement"),
            new TerrainAvecPlan("Salle A", "Salle couverte climatisée")
        );
        for (TerrainAvecPlan t : terrains) {
            t.saveInDB(con);
        }

        // Types de jeu (Extensions 4-5)
        System.out.println("- Types de jeu...");
        TypeJeu.creerTypesJeuExemples(con);

        // Associer quelques types de jeu aux terrains
        List<TypeJeu> typesJeu = TypeJeu.tousLesTypesJeu(con);
        if (!typesJeu.isEmpty() && !terrains.isEmpty()) {
            // Tennis sur tous les courts
            TypeJeu tennis = typesJeu.stream()
                .filter(tj -> tj.getNom().contains("Tennis"))
                .findFirst()
                .orElse(null);
            
            if (tennis != null) {
                for (int i = 0; i < Math.min(3, terrains.size()); i++) {
                    tennis.associerTerrain(con, terrains.get(i).getId());
                }
            }
        }

        // Templates (Extension 6)
        System.out.println("- Templates de tournoi...");
        TemplateTournoi.creerTemplatesExemples(con);

        // Tournois (Extension 2)
        System.out.println("- Tournois multi...");
        
        // Tournoi actif
        TournoiMulti tournoiActif = new TournoiMulti(
            "Championnat Printemps 2025",
            4,
            2
        );
        tournoiActif.saveInDB(con);
        tournoiActif.demarrer(con);
        
        // Inscrire des joueurs
        for (Joueur j : joueurs) {
            tournoiActif.inscrireJoueur(con, j);
        }

        // Tournoi terminé (pour tester le classement global)
        TournoiMulti tournoiPasse = new TournoiMulti(
            "Tournoi d'Hiver 2024",
            3,
            2
        );
        tournoiPasse.saveInDB(con);
        tournoiPasse.demarrer(con);
        
        // Inscrire quelques joueurs
        for (int i = 0; i < 4; i++) {
            tournoiPasse.inscrireJoueur(con, joueurs.get(i));
        }
        
        // Simuler la fin du tournoi
        tournoiPasse.terminer(con);

        System.out.println("✓ Données de test créées");
    }

    /**
     * Supprime toutes les tables (base + extensions)
     */
    public static void supprimerTout(Connection con) throws SQLException {
        System.out.println("=== Suppression de toutes les tables ===");
        
        try (var st = con.createStatement()) {
            // Ordre important à cause des clés étrangères
            
            // Extensions
            try { st.executeUpdate("DROP TABLE IF EXISTS template_type_jeu"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS template_terrain"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS template_tournoi"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS terrain_type_jeu"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS type_jeu"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS classement_global"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS inscription_tournoi"); } 
            catch (SQLException e) {}
            
            // Tables de base
            try { st.executeUpdate("DROP TABLE IF EXISTS match_joueur"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS equipe"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS matchs"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS ronde"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS terrain"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS joueur"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS tournoi"); } 
            catch (SQLException e) {}
            
            try { st.executeUpdate("DROP TABLE IF EXISTS utilisateur"); } 
            catch (SQLException e) {}
        }
        
        System.out.println("✓ Toutes les tables supprimées");
    }

    /**
     * RAZ complète : supprime tout et recrée
     */
    public static void razComplete(Connection con) throws SQLException {
        supprimerTout(con);
        creerSchemaComplet(con);
        creerDonneesTest(con);
    }

    /**
     * Main de test
     */
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("║  INITIALISATION COMPLÈTE DES EXTENSIONS   ║");
            System.out.println("╚════════════════════════════════════════════╝\n");

            razComplete(con);

            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║         INITIALISATION TERMINÉE !          ║");
            System.out.println("╚════════════════════════════════════════════╝");
            System.out.println("\nExtensions implémentées :");
            System.out.println("  ✓ Extension 1 : Interface spécifique pour les joueurs");
            System.out.println("  ✓ Extension 2 : Gestion multi-tournoi");
            System.out.println("  ✓ Extension 3 : Gestion simple des terrains avec plan");
            System.out.println("  ✓ Extension 4 : Souplesse sur les jeux (nb équipes variable)");
            System.out.println("  ✓ Extension 5 : Intégration terrains/jeux");
            System.out.println("  ✓ Extension 6 : Facilité de création d'un nouveau tournoi");
            
        } catch (SQLException ex) {
            System.err.println("❌ Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}