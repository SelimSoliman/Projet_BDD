package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Configuration spécifique pour le Padel
 */
public class PadelConfiguration {

    /**
     * Crée les types de jeu spécifiques au Padel
     */
    public static void creerTypesJeuPadel(Connection con) throws SQLException {
        System.out.println("=== Création des types de jeu Padel ===");
        
        // Padel Double (le plus courant)
        TypeJeu padelDouble = new TypeJeu(
            "Padel Double",
            2,    // 2 équipes
            2,    // 2 joueurs par équipe (obligatoire)
            2     // 2 joueurs par équipe (obligatoire)
        );
        padelDouble.saveInDB(con);
        System.out.println("✓ Padel Double créé (2 équipes de 2 joueurs)");

        // Padel Simple (moins courant, mais existe)
        TypeJeu padelSimple = new TypeJeu(
            "Padel Simple",
            2,    // 2 équipes
            1,    // 1 joueur par équipe
            1     // 1 joueur par équipe
        );
        padelSimple.saveInDB(con);
        System.out.println("✓ Padel Simple créé (2 équipes de 1 joueur)");
        
        System.out.println("✓ Types de jeu Padel créés avec succès !");
    }

    /**
     * Associe les types de jeu Padel à tous les terrains disponibles
     */
    public static void associerPadelAuxTerrains(Connection con) throws SQLException {
        System.out.println("\n=== Association Padel ↔ Terrains ===");
        
        // Récupérer tous les types Padel
        List<TypeJeu> typesPadel = TypeJeu.tousLesTypesJeu(con).stream()
            .filter(tj -> tj.getNom().contains("Padel"))
            .toList();
        
        // Récupérer tous les terrains
        List<Terrain> terrains = Terrain.tousLesTerrains(con);
        
        // Associer chaque type Padel à chaque terrain
        for (TypeJeu typePadel : typesPadel) {
            for (Terrain terrain : terrains) {
                try {
                    typePadel.associerTerrain(con, terrain.getId());
                    System.out.println("✓ " + typePadel.getNom() + " → " + terrain.getNom());
                } catch (SQLException e) {
                    // Ignore si l'association existe déjà
                    if (!e.getMessage().contains("Duplicate")) {
                        throw e;
                    }
                }
            }
        }
        
        System.out.println("✓ Associations créées !");
    }

    /**
     * Crée des terrains spécifiques au Padel avec descriptions
     */
    public static void creerTerrainsPadel(Connection con) throws SQLException {
        System.out.println("\n=== Création des terrains Padel ===");
        
        List<TerrainAvecPlan> terrainsPadel = List.of(
            new TerrainAvecPlan(
                "Court Padel Central",
                "Terrain principal avec gradins, éclairage LED et murs en verre"
            ),
            new TerrainAvecPlan(
                "Court Padel 2",
                "Terrain annexe extérieur, gazon synthétique dernière génération"
            ),
            new TerrainAvecPlan(
                "Court Padel 3",
                "Terrain couvert, idéal pour jouer par mauvais temps"
            ),
            new TerrainAvecPlan(
                "Court Padel 4",
                "Terrain d'entraînement avec système de rebond variable"
            )
        );
        
        for (TerrainAvecPlan terrain : terrainsPadel) {
            terrain.saveInDB(con);
            System.out.println("✓ " + terrain.getNom() + " créé");
        }
        
        System.out.println("✓ Terrains Padel créés !");
    }

    /**
     * Configuration complète pour un tournoi de Padel
     */
    public static void configurationCompletePadel(Connection con) throws SQLException {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   CONFIGURATION TOURNOI PADEL         ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // 1. Créer les terrains
        creerTerrainsPadel(con);
        
        // 2. Créer les types de jeu
        creerTypesJeuPadel(con);
        
        // 3. Associer types de jeu et terrains
        associerPadelAuxTerrains(con);
        
        System.out.println("\n✅ Configuration Padel terminée !");
        System.out.println("\nVous pouvez maintenant :");
        System.out.println("  • Créer un tournoi Padel Double (4 joueurs/match)");
        System.out.println("  • Créer un tournoi Padel Simple (2 joueurs/match)");
        System.out.println("  • Utiliser les 4 courts disponibles");
    }

    /**
     * Crée un template de tournoi Padel prêt à l'emploi
     */
    public static TemplateTournoi creerTemplatePadel(Connection con, String typePadel) 
            throws SQLException {
        
        int joueursParEquipe = typePadel.equals("Double") ? 2 : 1;
        
        TemplateTournoi template = new TemplateTournoi(
            "Tournoi Padel " + typePadel,
            "Configuration standard pour un tournoi de Padel " + typePadel,
            4,                  // 4 terrains
            joueursParEquipe,   // 2 pour Double, 1 pour Simple
            20,                 // 20 minutes par match
            true                // Template public
        );
        
        // Associer tous les terrains Padel
        List<Terrain> terrains = Terrain.tousLesTerrains(con).stream()
            .filter(t -> t.getNom().contains("Padel"))
            .toList();
        
        for (Terrain t : terrains) {
            template.ajouterTerrain(t.getId());
        }
        
        // Associer le type de jeu correspondant
        TypeJeu typeJeu = TypeJeu.tousLesTypesJeu(con).stream()
            .filter(tj -> tj.getNom().equals("Padel " + typePadel))
            .findFirst()
            .orElse(null);
        
        if (typeJeu != null) {
            template.ajouterTypeJeu(typeJeu.getId());
        }
        
        // Sauvegarder le template avec toutes ses associations
        template.saveComplete(con);
        
        System.out.println("✓ Template 'Tournoi Padel " + typePadel + "' créé");
        return template;
    }

    /**
     * Main de test pour configuration Padel
     */
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            
            // RAZ base de données
            System.out.println("Réinitialisation de la base...");
            InitExtensions.razComplete(con);
            
            // Configuration Padel
            configurationCompletePadel(con);
            
            // Créer les templates
            System.out.println("\n=== Création des templates ===");
            creerTemplatePadel(con, "Double");
            creerTemplatePadel(con, "Simple");
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║    CONFIGURATION PADEL TERMINÉE !     ║");
            System.out.println("╚════════════════════════════════════════╝");
            
        } catch (SQLException ex) {
            System.err.println("❌ Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}