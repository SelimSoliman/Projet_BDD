package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Interface console pour tester rapidement la logique métier
 * Alternative légère à Vaadin pour le développement/debug
 * 
 * UTILISATION : Lancer le main() pour tester en mode texte
 */
public class MainConsole {

    private Connection con;
    private Scanner in;
    private TournoiMulti tournoiCourant;
    private Utilisateur utilisateurCourant;

    public MainConsole() throws SQLException {
        this.con = ConnectionSimpleSGBD.defaultCon();
        this.in = new Scanner(System.in);
    }

    public static void main(String[] args) {
        try {
            MainConsole app = new MainConsole();
            
            // Menu d'initialisation
            app.menuInitialisation();
            
            // Login
            app.login();
            
            // Menu principal
            app.menuPrincipal();
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ==================== MENU INITIALISATION ====================
    
    private void menuInitialisation() throws SQLException {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║     TOURNOI PADEL - MODE CONSOLE          ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        System.out.println("1. Initialiser la base de données (RAZ complète)");
        System.out.println("2. Configuration Padel (recommandé)");
        System.out.println("3. Continuer avec la base existante");
        System.out.print("\nVotre choix : ");
        
        int choix = Integer.parseInt(in.nextLine());
        
        switch (choix) {
            case 1 -> {
                System.out.println("\n⚠️  RAZ complète de la base...");
                InitExtensions.razComplete(con);
                System.out.println("✅ Base réinitialisée !");
            }
            case 2 -> {
                System.out.println("\n🎾 Configuration Padel...");
                InitExtensions.razComplete(con);
                PadelConfiguration.configurationCompletePadel(con);
                System.out.println("✅ Configuration Padel terminée !");
            }
            case 3 -> {
                System.out.println("\n✅ Utilisation de la base existante");
            }
            default -> {
                System.out.println("Choix invalide, utilisation de la base existante");
            }
        }
    }

    // ==================== LOGIN ====================
    
    private void login() throws SQLException {
        while (true) {
            System.out.println("\n=== Connexion ===");
            System.out.print("Surnom : ");
            String surnom = in.nextLine();
            System.out.print("Mot de passe : ");
            String pass = in.nextLine();

            var opt = Utilisateur.findBySurnomPass(con, surnom, pass);
            if (opt.isPresent()) {
                utilisateurCourant = opt.get();
                System.out.println("✅ Connecté en tant que " + surnom +
                        " (role = " + (estAdmin() ? "admin" : "utilisateur") + ")");
                break;
            } else {
                System.out.println("❌ Identifiants incorrects\n");
            }
        }
    }

    private boolean estAdmin() {
        return utilisateurCourant != null && utilisateurCourant.getRole() == 1;
    }

    // ==================== MENU PRINCIPAL ====================
    
    private void menuPrincipal() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║          MENU PRINCIPAL                    ║");
            System.out.println("╚════════════════════════════════════════════╝");
            
            if (estAdmin()) {
                System.out.println("1. Créer un tournoi depuis template");
                System.out.println("2. Gérer les joueurs");
                System.out.println("3. Gérer les rondes / matchs");
                System.out.println("4. Gérer les terrains");
            }
            System.out.println("5. Consulter les informations");
            System.out.println("0. Quitter");
            System.out.print("\nVotre choix : ");

            try {
                choix = Integer.parseInt(in.nextLine());
            } catch (NumberFormatException ex) {
                choix = -1;
            }

            switch (choix) {
                case 1 -> { if (estAdmin()) creerTournoiDepuisTemplate(); }
                case 2 -> { if (estAdmin()) menuJoueurs(); }
                case 3 -> { if (estAdmin()) menuRondesMatchs(); }
                case 4 -> { if (estAdmin()) menuTerrains(); }
                case 5 -> afficherInfosTournoi();
                case 0 -> System.out.println("\n👋 Au revoir !");
                default -> System.out.println("❌ Choix invalide");
            }
        }
    }

    // ==================== CRÉATION TOURNOI DEPUIS TEMPLATE ====================
    
    private void creerTournoiDepuisTemplate() {
        try {
            // Lister les templates
            List<TemplateTournoi> templates = TemplateTournoi.getTousLesTemplates(con);
            
            if (templates.isEmpty()) {
                System.out.println("\n❌ Aucun template disponible");
                System.out.println("💡 Lancez d'abord PadelConfiguration.configurationCompletePadel()");
                return;
            }
            
            System.out.println("\n=== Templates disponibles ===");
            for (int i = 0; i < templates.size(); i++) {
                TemplateTournoi t = templates.get(i);
                System.out.println((i + 1) + ". " + t.getNom() + 
                                 " (" + t.getNbTerrains() + " terrains, " +
                                 t.getNbJoueursParEquipe() + " joueurs/équipe)");
            }
            
            System.out.print("\nChoisir un template (1-" + templates.size() + ") : ");
            int choix = Integer.parseInt(in.nextLine()) - 1;
            
            if (choix < 0 || choix >= templates.size()) {
                System.out.println("❌ Choix invalide");
                return;
            }
            
            TemplateTournoi template = templates.get(choix);
            
            System.out.print("Nom du nouveau tournoi : ");
            String nomTournoi = in.nextLine();
            
            TournoiMulti tournoi = template.creerTournoiDepuisTemplate(con, nomTournoi);
            this.tournoiCourant = tournoi;
            
            System.out.println("\n✅ Tournoi créé : " + nomTournoi);
            System.out.println("   - " + tournoi.getNbTerrains() + " terrains");
            System.out.println("   - " + tournoi.getNbJoueursParEquipe() + " joueurs par équipe");
            
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("❌ Erreur : " + ex.getMessage());
        }
    }

    // ==================== JOUEURS ====================
    
    private void menuJoueurs() {
        System.out.println("\n=== Gestion Joueurs ===");
        System.out.println("1. Ajouter un joueur");
        System.out.println("2. Lister les joueurs");
        System.out.print("Votre choix : ");
        
        try {
            int choix = Integer.parseInt(in.nextLine());
            switch (choix) {
                case 1 -> ajouterJoueur();
                case 2 -> listerJoueurs();
            }
        } catch (Exception ex) {
            System.out.println("❌ Erreur : " + ex.getMessage());
        }
    }

    private void ajouterJoueur() throws SQLException {
        System.out.print("Surnom : ");
        String surnom = in.nextLine();
        
        System.out.print("Nom : ");
        String nom = in.nextLine();
        
        System.out.print("Prénom : ");
        String prenom = in.nextLine();
        
        System.out.print("Catégorie (Débutant/Intermédiaire/Avancé/Expert) : ");
        String categorie = in.nextLine();
        
        System.out.print("Taille (cm) : ");
        int taille = Integer.parseInt(in.nextLine());
        
        System.out.print("Sexe (M/F) : ");
        String sexe = in.nextLine();

        Joueur j = new Joueur(surnom, categorie, taille, nom, prenom, sexe, LocalDate.now());
        j.saveInDB(con);
        
        System.out.println("✅ Joueur ajouté !");
    }

    private void listerJoueurs() throws SQLException {
        List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
        
        if (joueurs.isEmpty()) {
            System.out.println("❌ Aucun joueur en base");
            return;
        }
        
        System.out.println("\n=== Liste des joueurs ===");
        for (Joueur j : joueurs) {
            System.out.println("• " + j.getSurnom() + 
                             " (" + j.getNom() + " " + j.getPrenom() + 
                             ") - " + j.getCategorie());
        }
    }

    // ==================== RONDES / MATCHS ====================
    
    private void menuRondesMatchs() {
        if (tournoiCourant == null) {
            System.out.println("❌ Créez d'abord un tournoi");
            return;
        }
        
        System.out.println("\n=== Rondes / Matchs ===");
        System.out.println("1. Créer une nouvelle ronde");
        System.out.println("2. Saisir résultat d'un match");
        System.out.print("Votre choix : ");
        
        try {
            int choix = Integer.parseInt(in.nextLine());
            switch (choix) {
                case 1 -> creerRonde();
                case 2 -> saisirResultat();
            }
        } catch (Exception ex) {
            System.out.println("❌ Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void creerRonde() {
        System.out.println("\n⚠️  Fonctionnalité à implémenter");
        System.out.println("💡 Utilisez l'interface web Vaadin pour créer des rondes");
    }

    private void saisirResultat() {
        System.out.println("\n⚠️  Fonctionnalité à implémenter");
        System.out.println("💡 Utilisez l'interface web Vaadin pour saisir les résultats");
    }

    // ==================== TERRAINS ====================
    
    private void menuTerrains() {
        try {
            List<Terrain> terrains = Terrain.tousLesTerrains(con);
            
            if (terrains.isEmpty()) {
                System.out.println("\n❌ Aucun terrain en base");
                return;
            }
            
            System.out.println("\n=== Terrains disponibles ===");
            for (Terrain t : terrains) {
                System.out.println("• " + t.getNom() + 
                                 " - " + (t.estDisponible() ? "✅ Disponible" : "❌ Occupé"));
            }
            
        } catch (SQLException ex) {
            System.out.println("❌ Erreur : " + ex.getMessage());
        }
    }

    // ==================== CONSULTATION ====================
    
    private void afficherInfosTournoi() {
        try {
            // Récupérer tous les tournois
            List<TournoiMulti> tournois = TournoiMulti.tousLesTournois(con);
            
            if (tournois.isEmpty()) {
                System.out.println("\n❌ Aucun tournoi créé");
                return;
            }
            
            // Afficher le tournoi courant ou le dernier tournoi
            TournoiMulti tournoi = tournoiCourant != null ? tournoiCourant : tournois.get(0);
            
            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║        INFORMATIONS TOURNOI                ║");
            System.out.println("╚════════════════════════════════════════════╝");
            System.out.println("Nom : " + tournoi.getNom());
            System.out.println("Statut : " + tournoi.getStatut());
            System.out.println("Terrains : " + tournoi.getNbTerrains());
            System.out.println("Joueurs par équipe : " + tournoi.getNbJoueursParEquipe());
            System.out.println("Date création : " + tournoi.getDateCreation().toLocalDate());
            
            if (tournoi.getDateDebut() != null) {
                System.out.println("Date début : " + tournoi.getDateDebut().toLocalDate());
            }
            
            if (tournoi.getDateFin() != null) {
                System.out.println("Date fin : " + tournoi.getDateFin().toLocalDate());
            }
            
            // Compter les joueurs inscrits à ce tournoi
            int nbJoueurs = tournoi.getJoueursInscrits(con).size();
            System.out.println("Joueurs inscrits : " + nbJoueurs);
            
            // Si plusieurs tournois existent, afficher un résumé
            if (tournois.size() > 1) {
                System.out.println("\n📊 Nombre total de tournois : " + tournois.size());
                System.out.println("   - En cours : " + tournois.stream()
                    .filter(t -> t.getStatut() == TournoiMulti.StatutTournoi.EN_COURS)
                    .count());
                System.out.println("   - Terminés : " + tournois.stream()
                    .filter(t -> t.getStatut() == TournoiMulti.StatutTournoi.TERMINE)
                    .count());
            }
            
        } catch (SQLException ex) {
            System.out.println("❌ Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}