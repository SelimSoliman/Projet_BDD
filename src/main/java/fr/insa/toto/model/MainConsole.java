package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
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
            app.login();          // identification + role
            app.menuPrincipal();  // menus selon le role
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
                        " (role = " + (estAdmin() ? "admin" : "utilisateur") + ")");
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
            System.out.println("Fonctionnalite reservee a un administrateur.");
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
                System.out.println("2. Definir les parametres du tournoi");
                System.out.println("3. Gerer les joueurs");
                System.out.println("4. Gerer les rondes / matchs");
                System.out.println("5. Gerer les terrains");
                System.out.println("6. Gerer les equipes");
            }
            System.out.println("7. Consulter les informations du tournoi");
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
                case 6 -> ifAdmin(this::menuEquipes);
                case 7 -> afficherInfosTournoi();
                case 0 -> System.out.println("Au revoir.");
                default -> System.out.println("Choix invalide");
            }
        }
    }

    // ================== BD / SCHEMA ==================

    private void razBdd() {
        try {
            GestionBDD.razBdd(con);
            this.tournoiCourant = null;
            System.out.println("Base recreee.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ================== TOURNOI UNIQUE ==================

    private void creerTournoiFixe() {
        if (tournoiCourant != null) {
            System.out.println("Un tournoi existe deja.");
            return;
        }
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
            System.out.println("3. Supprimer un joueur");
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
                case 3 -> supprimerJoueurConsole();
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

        System.out.print("Nom : ");
        String nom = in.nextLine();

        System.out.print("Prenom : ");
        String prenom = in.nextLine();

        System.out.print("Sexe (M/F) : ");
        String sexe = in.nextLine();

        System.out.print("Date de naissance (AAAA-MM-JJ, laisser vide si inconnu) : ");
        String dateStr = in.nextLine();
        LocalDate naissance = null;
        if (!dateStr.isBlank()) {
            naissance = LocalDate.parse(dateStr);
        }

        Joueur j = new Joueur(
                surnom,
                categorie,
                taillecm,
                nom,
                prenom,
                sexe,
                naissance
        );

        try {
            j.saveInDB(con);
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
                            + " | " + j.getNom()
                            + " " + j.getPrenom()
                            + " | cat: " + j.getCategorie()
                            + " | taille: " + j.getTaillecm() + " cm"
                            + " | sexe: " + j.getSexe()
                            + " | ne le: " + j.getDateNaissance());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void supprimerJoueurConsole() {
        if (tournoiCourant == null) {
            System.out.println("Aucun tournoi courant.");
            return;
        }

        listerJoueursTournoi();

        System.out.print("Id du joueur a supprimer : ");
        int id;
        try {
            id = Integer.parseInt(in.nextLine());
        } catch (NumberFormatException ex) {
            System.out.println("Id invalide.");
            return;
        }

        try {
            Joueur.supprimer(con, id);   // à implémenter dans Joueur
            System.out.println("Joueur " + id + " supprime.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de la suppression du joueur.");
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
        System.out.println("2. Saisir le resultat d'un match (a completer)");
        System.out.println("3. Clore la derniere ronde");
        System.out.println("4. Lister les rondes");
        System.out.println("5. Gerer les matchs (CRUD)");
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
            case 3 -> cloreDerniereRonde();
            case 4 -> listerRondes();
            case 5 -> menuMatchs();
            case 0 -> { }
            default -> System.out.println("Choix invalide");
        }
    }
}
private void menuMatchs() {
    if (tournoiCourant == null) {
        System.out.println("Aucun tournoi courant.");
        return;
    }
    int choix = -1;
    while (choix != 0) {
        System.out.println("\n=== Matchs (admin) ===");
        System.out.println("1. Lister les matchs d'une ronde");
        System.out.println("2. Creer un match manuellement");
    
        System.out.println("3. Supprimer un match");
        System.out.println("0. Retour");
        System.out.print("Votre choix : ");
        try {
            choix = Integer.parseInt(in.nextLine());
        } catch (NumberFormatException ex) {
            choix = -1;
        }
        switch (choix) {
            case 1 -> listerMatchsRonde();
            case 2 -> creerMatchManuel();
            
            case 3 -> supprimerMatch();
            case 0 -> { }
            default -> System.out.println("Choix invalide");
        }
    }
}
private Ronde choisirRonde() {
    if (tournoiCourant.getRondes().isEmpty()) {
        System.out.println("Aucune ronde.");
        return null;
    }
    listerRondes();
    System.out.print("Numero de la ronde : ");
    int num;
    try {
        num = Integer.parseInt(in.nextLine());
    } catch (NumberFormatException ex) {
        System.out.println("Numero invalide.");
        return null;
    }
    for (Ronde r : tournoiCourant.getRondes()) {
        if (r.getNumero() == num) {
            return r;
        }
    }
    System.out.println("Ronde " + num + " introuvable.");
    return null;
}

private void listerMatchsRonde() {
    Ronde r = choisirRonde();
    if (r == null) return;
    afficherMatchsRonde(r);
}
private void creerMatchManuel() {
    // choisir la ronde
    Ronde r = choisirRonde();
    if (r == null) return;

    // choisir un terrain
    if (tournoiCourant.getTerrains().isEmpty()) {
        System.out.println("Pas de terrain, creez-en d'abord.");
        return;
    }
    listerTerrains();
    System.out.print("Id du terrain pour ce match : ");
    int idTerrain;
    try {
        idTerrain = Integer.parseInt(in.nextLine());
    } catch (NumberFormatException ex) {
        System.out.println("Id invalide.");
        return;
    }
    Terrain terrainChoisi = null;
    for (Terrain t : tournoiCourant.getTerrains()) {
        if (t.getId() == idTerrain) {
            terrainChoisi = t;
            break;
        }
    }
    if (terrainChoisi == null) {
        System.out.println("Terrain introuvable.");
        return;
    }

    // creation du match : les equipes sont creees dans le constructeur Match
    try {
        Match m = new Match(r, terrainChoisi);
        m.saveInDB(con);      // enregistre le match en BD
        r.ajouterMatch(m);    // rattache le match a la ronde en memoire

        System.out.println("Match cree (id=" + m.getId()
                + ") sur terrain " + terrainChoisi.getNom());
    } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("Erreur lors de la creation du match.");
    }
}
// selection d un match dans une ronde
private Match choisirMatchDansRonde(Ronde r) {
    if (r.getMatchs().isEmpty()) {
        System.out.println("Aucun match dans cette ronde.");
        return null;
    }
    afficherMatchsRonde(r);
    System.out.print("Id du match : ");
    int id;
    try {
        id = Integer.parseInt(in.nextLine());
    } catch (NumberFormatException ex) {
        System.out.println("Id invalide.");
        return null;
    }
    for (Match m : r.getMatchs()) {
        if (m.getId() == id) {
            return m;
        }
    }
    System.out.println("Match introuvable.");
    return null;
}

// suppression d un match
private void supprimerMatch() {
    Ronde r = choisirRonde();
    if (r == null) return;

    Match m = choisirMatchDansRonde(r);   // bien le meme nom que ci-dessus
    if (m == null) return;

    try {
        Match.supprimer(con, m.getId());  // methode statique dans Match
        r.getMatchs().remove(m);          // retire du modele en memoire
        System.out.println("Match " + m.getId() + " supprime.");
    } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("Erreur lors de la suppression du match.");
    }
}



private void creerRondeSimple() {
    Ronde r;
    try {
        r = tournoiCourant.nouvelleRonde();
    } catch (IllegalStateException e) {
        System.out.println(e.getMessage());
        return;
    }

    try {
        r.saveInDB(con);

        // recharger joueurs
        tournoiCourant.clearJoueurs();
        for (Joueur j : Joueur.tousLesJoueurs(con)) {
            tournoiCourant.ajouterJoueur(j);
        }

        // recharger terrains
        tournoiCourant.clearTerrains();
        for (Terrain t : Terrain.tousLesTerrains(con)) {
            tournoiCourant.ajouterTerrain(t);
        }

        tournoiCourant.creerMatchsPourRonde(r, 2, con);

        System.out.println("Ronde " + r.getNumero() + " creee avec "
                + r.getMatchs().size() + " match(s).");
        afficherMatchsRonde(r);

    } catch (SQLException e) {
        e.printStackTrace();
    } catch (IllegalStateException e) {
        System.out.println("Impossible de creer les matchs : " + e.getMessage());
    }
}



    private void afficherMatchsRonde(Ronde r) {
        System.out.println("\nMatchs de la ronde " + r.getNumero() + " :");
        if (r.getMatchs().isEmpty()) {
            System.out.println("(aucun match)");
            return;
        }
        for (Match m : r.getMatchs()) {
            Terrain t = m.getTerrain();
            String nomTerrain = (t == null) ? "sans terrain" : t.getNom();

            System.out.println("- Match " + m.getId() + " sur terrain " + nomTerrain);

            System.out.print("  Equipe 1 : ");
            for (Joueur j : m.getEquipe1().getJoueurs()) {
                System.out.print(j.getSurnom() + " ");
            }
            System.out.println();

            System.out.print("  Equipe 2 : ");
            for (Joueur j : m.getEquipe2().getJoueurs()) {
                System.out.print(j.getSurnom() + " ");
            }
            System.out.println();
        }
    }

    private void saisirResultatMatch() {
        System.out.println("Saisie de resultat de match : a implementer.");
    }

    private void cloreDerniereRonde() {
        List<Ronde> rondes = tournoiCourant.getRondes();
        if (rondes.isEmpty()) {
            System.out.println("Aucune ronde.");
            return;
        }

        Ronde derniere = rondes.get(rondes.size() - 1);
        if (derniere.isClose()) {
            System.out.println("La ronde " + derniere.getNumero() + " est deja close.");
            return;
        }

        derniere.clore();

        try {
            derniere.updateInDB(con);
            System.out.println("Ronde " + derniere.getNumero() + " close.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de la mise a jour de la ronde en base.");
        }
    }

    private void listerRondes() {
        if (tournoiCourant == null) {
            System.out.println("Aucun tournoi courant.");
            return;
        }
        if (tournoiCourant.getRondes().isEmpty()) {
            System.out.println("Aucune ronde.");
            return;
        }
        System.out.println("\nRondes du tournoi :");
        for (Ronde r : tournoiCourant.getRondes()) {
            System.out.println("Ronde " + r.getNumero()
                    + " | debut: " + r.getDebut()
                    + " | statut: " + (r.isClose() ? "close" : "en cours")
                    + " | nb matchs: " + r.getMatchs().size());
        }
    }

    // ================== TERRAINS (ADMIN) ==================

    private void menuTerrains() {
        int choix = -1;
        while (choix != 0) {
            System.out.println("\n=== Terrains (admin) ===");
            System.out.println("1. Creer un terrain");
            System.out.println("2. Lister les terrains");
            System.out.println("3. Supprimer un terrain");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");
            try {
                choix = Integer.parseInt(in.nextLine());
            } catch (NumberFormatException ex) {
                choix = -1;
            }

            switch (choix) {
                case 1 -> creerTerrain();
                case 2 -> listerTerrains();
                case 3 -> supprimerTerrainConsole();
                case 0 -> { }
                default -> System.out.println("Choix invalide");
            }
        }
    }

    private void supprimerTerrainConsole() {
        if (tournoiCourant == null) {
            System.out.println("Aucun tournoi courant.");
            return;
        }

        listerTerrains();

        System.out.print("Id du terrain a supprimer : ");
        int id;
        try {
            id = Integer.parseInt(in.nextLine());
        } catch (NumberFormatException ex) {
            System.out.println("Id invalide.");
            return;
        }

        try {
            Terrain.supprimer(con, id);   // à implémenter dans Terrain
            System.out.println("Terrain " + id + " supprime.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de la suppression du terrain.");
        }
    }

   private void listerTerrains() {
    try {
        List<Terrain> terrains = Terrain.tousLesTerrains(con);
        if (terrains.isEmpty()) {
            System.out.println("Terrains du tournoi :");
            System.out.println("(aucun terrain en base)");
            return;
        }
        System.out.println("Terrains du tournoi :");
        for (Terrain t : terrains) {
            System.out.println(" " + t.getId() + " | " + t.getNom()
                    + " | " + (t.estDisponible() ? "disponible" : "occupe"));
        }
    } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("Erreur lors de la recuperation des terrains.");
    }
}




    private void creerTerrain() {
        System.out.print("Nom du terrain : ");
        String nom = in.nextLine();
        Terrain t = new Terrain(nom);
        try {
            t.saveInDB(con);
            System.out.println("Terrain cree : " + t);
            if (tournoiCourant != null) {
                tournoiCourant.ajouterTerrain(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================== EQUIPES ==================

   private void menuEquipes() {
    if (tournoiCourant == null) {
        System.out.println("Aucun tournoi courant.");
        return;
    }
    int choix = -1;
    while (choix != 0) {
        System.out.println("\n=== Equipes (admin) ===");
        System.out.println("1. Lister toutes les equipes");
        System.out.println("2. Supprimer une equipe");
        System.out.println("0. Retour");
        System.out.print("Votre choix : ");
        try {
            choix = Integer.parseInt(in.nextLine());
        } catch (NumberFormatException ex) {
            choix = -1;
        }
        switch (choix) {
            case 1 -> listerEquipes();
            case 2 -> supprimerEquipe();
            case 0 -> { }
            default -> System.out.println("Choix invalide");
        }
    }
}


   
private void supprimerEquipe() {
    listerEquipes();
    
    System.out.print("Id de l'equipe a supprimer : ");
    int id;
    try {
        id = Integer.parseInt(in.nextLine());
    } catch (NumberFormatException ex) {
        System.out.println("Id invalide.");
        return;
    }

    try {
        Equipe.supprimer(con, id);
        System.out.println("Equipe " + id + " supprimee.");
    } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("Erreur lors de la suppression de l'equipe.");
    }
}

    private void listerEquipes() {
    try {
        List<Equipe> equipes = Equipe.toutesLesEquipes(con);
        if (equipes.isEmpty()) {
            System.out.println("Aucune equipe en base.");
            return;
        }
        
        System.out.println("\n=== Liste des equipes ===");
        for (Equipe e : equipes) {
            System.out.print("Equipe " + e.getId() + " : ");
            if (e.getJoueurs().isEmpty()) {
                System.out.println("(vide)");
            } else {
                for (Joueur j : e.getJoueurs()) {
                    System.out.print(j.getSurnom() + " ");
                }
                System.out.println(" | score: " + e.getScoreTotal());
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("Erreur lors de la recuperation des equipes.");
    }
}

    // ================== CONSULTATION (ADMIN + UTILISATEUR) ==================

    private void afficherInfosTournoi() {
        if (tournoiCourant == null) {
            System.out.println("Aucun tournoi courant (creez-le cote admin).");
            return;
        }
        System.out.println("\n=== Informations sur le tournoi ===");
        System.out.println("Nom : " + tournoiCourant.getNom());
        System.out.println("Nombre de joueurs : " + tournoiCourant.getJoueurs().size());
        System.out.println("Nombre de rondes : " + tournoiCourant.getRondes().size());
    }
}
