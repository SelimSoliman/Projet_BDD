package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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
                System.out.println("Connecte en tant que " + surnom +
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
            System.out.println("Fonctionnalite reservee à un administrateur.");
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
                System.out.println("1. Reinitialiser la base (raz)");
                System.out.println("2. Definir les paramètres du tournoi");
                System.out.println("3. Gerer les joueurs");
                System.out.println("4. Gerer les rondes / matchs");
                System.out.println("5. Gerer les terrains");
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
            System.out.println("Base recreee.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ================== TOURNOI UNIQUE ==================

    private void creerTournoiFixe() {
        String nom = "Tournoi principal";
        System.out.print("Nombre de terrains : ");
        int nbTerrains = Integer.parseInt(in.nextLine());
        System.out.print("Nombre de joueurs par equipe : ");
        int nbJoueursParEquipe = Integer.parseInt(in.nextLine());

        this.tournoiCourant = new Tournoi(nom, nbTerrains);
        try {
            tournoiCourant.saveInDB(con);
            System.out.println("Tournoi '" + nom + "' cree et sauvegarde.");
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
            System.out.println("Creez d'abord le tournoi (option 2).");
            return;
        }

        System.out.print("Surnom : ");
        String surnom = in.nextLine();
        System.out.print("Categorie : ");
        String categorie = in.nextLine();
        System.out.print("Taille (en cm) : ");
        int taillecm = Integer.parseInt(in.nextLine());

        Joueur j = new Joueur(-1, surnom, categorie, taillecm);

        j.setSurnom(surnom);
        j.setCategorie(categorie);
        j.setTaillecm(taillecm);

        try {
            j.saveInDB(con);
            // facultatif : garder aussi une copie en memoire
            tournoiCourant.ajouterJoueur(j);
            System.out.println("Joueur ajoute.");
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
        try {
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            if (joueurs.isEmpty()) {
                System.out.println("(aucun joueur en base)");
            } else {
                for (Joueur j : joueurs) {
                    System.out.println(" " + j.getId()
                            + " | " + j.getSurnom()
                            + " | " + j.getCategorie()
                            + " | " + j.getTaillecm() + " cm");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== RONDES / MATCHS (ADMIN) ==================

    private void menuRondesMatchs() {
        if (tournoiCourant == null) {
            System.out.println("Creez d'abord le tournoi.");
            return;
        }
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n=== Rondes / Matchs (admin) ===");
            System.out.println("1. Creer une nouvelle ronde");
            System.out.println("2. Saisir le resultat d'un match (à completer)");
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
        Ronde r = tournoiCourant.nouvelleronde();
        try {
            r.saveInDB(con);
            System.out.println("Ronde " + r.getNumero() + " creee.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saisirResultatMatch() {
        System.out.println("Saisie de resultat de match : à implementer.");
    }

    // ================== TERRAINS (ADMIN) ==================

    private void menuTerrains() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n=== Terrains (admin) ===");
            System.out.println("1. Creer un terrain");
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
        Terrain t = new Terrain(-1, nom);
        try {
            t.saveInDB(con);
            System.out.println("Terrain cree : " + t);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== CONSULTATION (ADMIN + UTILISATEUR) ==================

    private void afficherInfosTournoi() {
        if (tournoiCourant == null) {
            System.out.println("Aucun tournoi courant (creez-le côte admin).");
            return;
        }
        System.out.println("\n=== Informations sur le tournoi ===");
        System.out.println("Nom : " + tournoiCourant.getNom());
        System.out.println("Nombre de joueurs : " + tournoiCourant.getJoueurs().size());
        System.out.println("Nombre de rondes : " + tournoiCourant.getRondes().size());
    }
}
