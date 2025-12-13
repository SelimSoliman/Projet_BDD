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
package fr.insa.toto.console;



import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Console {

    private Connection con;
    private Scanner in;
    private Tournoi tournoiCourant;

    public Console() throws SQLException {
        this.con = ConnectionSimpleSGBD.defaultCon();
        this.in = new Scanner(System.in);
    }

    public static void main(String[] args) {
        try {
            Console app = new Console();
            app.menuPrincipal();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void menuPrincipal() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("=== Gestion de tournoi (console) ===");
            System.out.println("1. Créer le schéma BD (raz)");
            System.out.println("2. Créer un tournoi");
            System.out.println("3. Gérer les joueurs");
            System.out.println("4. Gérer les rondes / matchs");
            System.out.println("5. Gérer les terrains");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");
            choix = Integer.parseInt(in.nextLine());

            switch (choix) {
                case 1 -> razBdd();
                case 2 -> creerTournoi();
                case 3 -> menuJoueurs();
                case 4 -> menuRondesMatchs();
                case 5 -> menuTerrains();
                case 0 -> System.out.println("Au revoir.");
                default -> System.out.println("Choix invalide");
            }
        }
    }

    // ---------- BD / schéma ----------

    private void razBdd() {
        try {
            GestionBDD.razBdd(con);
            System.out.println("Base recréée.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ---------- Tournoi ----------

    private void creerTournoi() {
        System.out.print("Nom du tournoi : ");
        String nom = in.nextLine();
        System.out.print("Nombre de terrains : ");
        int nbTerrains = Integer.parseInt(in.nextLine());
        System.out.print("Nombre de joueurs par équipe : ");
        int nbJoueursParEquipe = Integer.parseInt(in.nextLine());

        this.tournoiCourant = new Tournoi(nom, nbTerrains, nbJoueursParEquipe);
        try {
            tournoiCourant.saveInDB(con);
            System.out.println("Tournoi créé et sauvegardé.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Joueurs ----------

    private void menuJoueurs() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("=== Joueurs ===");
            System.out.println("1. Ajouter un joueur");
            System.out.println("2. Lister les joueurs du tournoi courant");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            choix = Integer.parseInt(in.nextLine());

            switch (choix) {
                case 1 -> ajouterJoueurConsole();
                case 2 -> listerJoueursTournoi();
                case 0 -> {}
                default -> System.out.println("Choix invalide");
            }
        }
    }

    private void ajouterJoueurConsole() {
        if (tournoiCourant == null) {
            System.out.println("Créez d'abord un tournoi.");
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

    // ---------- Rondes / matchs ----------

    private void menuRondesMatchs() {
        if (tournoiCourant == null) {
            System.out.println("Créez d'abord un tournoi.");
            return;
        }
        int choix = -1;
        while (choix != 0) {
            System.out.println("=== Rondes / Matchs ===");
            System.out.println("1. Créer une nouvelle ronde");
            System.out.println("2. Saisir le résultat d'un match");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            choix = Integer.parseInt(in.nextLine());

            switch (choix) {
                case 1 -> creerRondeSimple();
                case 2 -> saisirResultatMatch(); // à implémenter selon ta stratégie de stockage
                case 0 -> {}
                default -> System.out.println("Choix invalide");
            }
        }
    }

    private void creerRondeSimple() {
        // Ronde numéro = taille+1
        Ronde r = tournoiCourant.nouvelleronde();
        try {
            r.saveInDB(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Ici tu devras répartir les joueurs de tournoiCourant en équipes / matchs
        // Pour l'instant : on ne fait qu'afficher qu’une ronde est créée
        System.out.println("Ronde " + r.getNumero() + " créée.");
    }

    private void saisirResultatMatch() {
        // À faire : retrouver un match par id, lire scores, appeler definirScores(score1, score2), puis saveInDB(con)
        System.out.println("Saisie de résultat de match à implémenter selon ta logique d'accès BD.");
    }

    // ---------- Terrains ----------

    private void menuTerrains() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("=== Terrains ===");
            System.out.println("1. Créer un terrain");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            choix = Integer.parseInt(in.nextLine());

            switch (choix) {
                case 1 -> creerTerrain();
                case 0 -> {}
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
}
