package fr.insa.toto.console;

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class MainConsole {

    private Connection con;
    private Scanner in;
    private Tournoi tournoiCourant;
    private Utilisateur utilisateurCourant;

    // ================== CONSTRUCTEUR ==================

    public MainConsole() throws SQLException {
        this.con = ConnectionSimpleSGBD.defaultCon();
        this.in = new Scanner(System.in);
    }

    // ================== MAIN ==================

    public static void main(String[] args) {
        try {
            MainConsole app = new MainConsole();
            app.login();          // identification + rôle
            app.menuPrincipal();  // menus selon le rôle
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ================== AUTHENTIFICATION ==================

    private void login() throws SQLException {
        while (true) {
            System.out.println("=== Connexion ===");
            System.out.print("Surnom : ");
            String surnom = in.nextLine();
            System.out.print("Mot de passe : ");
            String pass = in.nextLine();

            var opt = Utilisateur.findBySurnomPass(con, surnom, pass);
            if (opt.isPresent()) {
                utilisateurCourant = opt.get();
                System.out.println("Connecté en tant que " + surnom +
                        " (rôle = " + (estAdmin() ? "admin" : "utilisateur") + ")");
                break;
            } else {
                System.out.println("Identifiants incorrects, recommencez.\n");
            }
        }
    }

    private boolean estAdmin() {
        return utilisateurCourant != null && utilisateurCourant.getRole() == 1;
    }

    private void ifAdmin(Runnable action) {
        if (!estAdmin()) {
            System.out.println("Fonctionnalité réservée à un administrateur.");
            return;
        }
        action.run();
    }

    // ================== MENU PRINCIPAL ==================

    private void menuPrincipal() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n=== Gestion de tournoi (console) ===");
            if (estAdmin()) {
                System.out.println("1. Réinitialiser la base (raz)");
                System.out.println("2. Définir les paramètres du tournoi");
                System.out.println("3. Gérer les joueurs");
                System.out.println("4. Gérer les rondes / matchs");
                System.out.println("5. Gérer les terrains");
            }
            System.out.println("6. Consulter les informations du tournoi");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");

            try {
                choix = Integer.parseInt(in.nextLine());
            } catch (NumberFormatException ex) {
                choix = -1;
            }

            switch (choix) {
                case 1 -> ifAdmin(this::razBdd);
                case 2 -> ifAdmin(this::creerTournoiFixe);
                case 3 -> ifAdmin(this::menuJoueurs);
                case 4 -> ifAdmin(this::menuRondesMatchs);
                case 5 -> ifAdmin(this::menuTerrains);
                case 6 -> afficherInfosTournoi();
                case 0 -> System.out.println("Au revoir.");
                default -> System.out.println("Choix invalide");
            }
        }
    }

    // ================== BD / SCHEMA ==================

    private void razBdd() {
        try {
            GestionBDD.razBdd(con);
            System.out.println("Base recréée.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ================== TOURNOI UNIQUE ==================

    private void creerTournoiFixe() {
        // Nom fixé : un seul tournoi
        String nom = "Tournoi principal";
        System.out.print("Nombre de terrains : ");
        int nbTerrains = Integer.parseInt(in.nextLine());
        System.out.print("Nombre de joueurs par équipe : ");
        int nbJoueursParEquipe = Integer.parseInt(in.nextLine());

        this.tournoiCourant = new Tournoi(nom, nbTerrains, nbJoueursParEquipe);
        try {
            tournoiCourant.saveInDB(con);
            System.out.println("Tournoi '" + nom + "' créé et sauvegardé.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== JOUEURS (ADMIN) ==================

    private void menuJoueurs() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n=== Joueurs (admin) ===");
            System.out.println("1. Ajouter un joueur");
            System.out.println("2. Lister les joueurs du tournoi courant");
            // plus tard : 3. Modifier, 4. Supprimer
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            try {
                choix = Integer.parseInt(in.nextLine());
            } catch (NumberFormatException ex) {
                choix = -1;
            }

            switch (choix) {
                case 1 -> ajouterJoueurConsole();
                case 2 -> listerJoueursTournoi();
                case 0 -> { }
                default -> System.out.println("Choix invalide");
            }
        }
    }

    private void ajouterJoueurConsole() {
        if (tournoiCourant == null) {
            System.out.println("Créez d'abord le tournoi (option 2).");
            return;
        }
        System.out.print("Nom : ");
        String nom = in.nextLine();
        System.out.print("Prénom : ");
        String prenom = in.nextLine();
        System.out.print("Surnom : ");
        String surnom = in.nextLine();
        System.out.print("Sexe : ");
        String sexe = in.nextLine();
        System.out.print("Âge : ");
        int age = Integer.parseInt(in.nextLine());

        Joueur j = new Joueur(0, nom, prenom);
        j.setSurnom(surnom);
        j.setSexe(sexe);
        j.setAge(age);

        try {
            j.saveInDB(con);
            tournoiCourant.ajouterJoueur(j);
            System.out.println("Joueur ajouté.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void listerJoueursTournoi() {
        if (tournoiCourant == null) {
            System.out.println("Aucun tournoi courant.");
            return;
        }
        System.out.println("Joueurs du tournoi " + tournoiCourant.getNom() + " :");
        for (Joueur j : tournoiCourant.getJoueurs()) {
            System.out.println("- " + j.getId() + " " + j.getPrenom() + " " + j.getNom());
        }
    }

    // ================== RONDES / MATCHS (ADMIN) ==================

    private void menuRondesMatchs() {
        if (tournoiCourant == null) {
            System.out.println("Créez d'abord le tournoi.");
            return;
        }
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n=== Rondes / Matchs (admin) ===");
            System.out.println("1. Créer une nouvelle ronde");
            System.out.println("2. Saisir le résultat d'un match (à compléter)");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            try {
                choix = Integer.parseInt(in.nextLine());
            } catch (NumberFormatException ex) {
                choix = -1;
            }

            switch (choix) {
                case 1 -> creerRondeSimple();
                case 2 -> saisirResultatMatch();
                case 0 -> { }
                default -> System.out.println("Choix invalide");
            }
        }
    }

    private void creerRondeSimple() {
        // Pour l’instant : crée juste une ronde vide
        Ronde r = tournoiCourant.nouvelleronde();
        try {
            r.saveInDB(con);
            System.out.println("Ronde " + r.getNumero() + " créée.");
            // TODO : créer les matchs de la ronde, répartir les joueurs aléatoirement
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saisirResultatMatch() {
        // À implémenter plus tard : sélection d’un match, saisie des scores, appel à definirScores(...)
        System.out.println("Saisie de résultat de match : à implémenter.");
    }

    // ================== TERRAINS (ADMIN) ==================

    private void menuTerrains() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n=== Terrains (admin) ===");
            System.out.println("1. Créer un terrain");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            try {
                choix = Integer.parseInt(in.nextLine());
            } catch (NumberFormatException ex) {
                choix = -1;
            }

            switch (choix) {
                case 1 -> creerTerrain();
                case 0 -> { }
                default -> System.out.println("Choix invalide");
            }
        }
    }

    private void creerTerrain() {
        System.out.print("Nom du terrain : ");
        String nom = in.nextLine();
        Terrain t = new Terrain(0, nom);
        try {
            t.saveInDB(con);
            System.out.println("Terrain créé : " + t);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== CONSULTATION (ADMIN + UTILISATEUR) ==================

    private void afficherInfosTournoi() {
        if (tournoiCourant == null) {
            System.out.println("Aucun tournoi courant (créez-le côté admin).");
            return;
        }
        System.out.println("\n=== Informations sur le tournoi ===");
        System.out.println("Nom : " + tournoiCourant.getNom());
        System.out.println("Nombre de joueurs : " + tournoiCourant.getJoueurs().size());
        System.out.println("Nombre de rondes : " + tournoiCourant.getRondes().size());
        // TODO : afficher classement joueurs, liste des rondes avec statut, etc.
    }
}
