package fr.insa.toto.webui;

import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import fr.insa.toto.webui.session.SessionInfo;

// vues utilisateurs
import fr.insa.toto.webui.utilisateurs.CreationAdmin;
import fr.insa.toto.webui.utilisateurs.ListeUtilisateurs;

// ✅ vues joueurs
import fr.insa.toto.webui.joueurs.CreationJoueurView;
import fr.insa.toto.webui.joueurs.ListeJoueursView;

// ✅ vues équipes
import fr.insa.toto.webui.extensions.GestionEquipesView;

// ✅ vues rondes/matchs
import fr.insa.toto.webui.extensions.GestionRondesMatchsView;
import fr.insa.toto.webui.extensions.GestionResultatsMatchsView;

// ✅ vue initialisation données
import fr.insa.toto.webui.extensions.InitialisationDonneesView;

// vues joueur espace perso
import fr.insa.toto.webui.joueurs.InterfaceJoueurView;

// vues tournois / extensions
import fr.insa.toto.webui.extensions.ListeTournoisView;
import fr.insa.toto.webui.extensions.GestionTerrainsView;
import fr.insa.toto.webui.extensions.CreationTerrainView;
import fr.insa.toto.webui.extensions.ListeTerrainsView;

// autres vues
import fr.insa.toto.webui.tournois.CreerTournoiView;

public class MainMenu extends SideNav {

    public MainMenu() {
        addClassName("sidebar");

        /* ===== ACCUEIL ===== */
        SideNavItem accueil = new SideNavItem("Accueil", VuePrincipale.class);

        /* ===== UTILISATEURS ===== */
        SideNavItem utilisateurs = new SideNavItem("Utilisateurs");
        utilisateurs.addItem(new SideNavItem("Liste", ListeUtilisateurs.class));

        if (SessionInfo.adminConnected()) {
            utilisateurs.addItem(new SideNavItem("Création", CreationAdmin.class));
        }

        /* ===== ✅ JOUEURS ===== */
        SideNavItem joueurs = new SideNavItem("Joueurs");
       if (SessionInfo.adminConnected()) {
    joueurs.addItem(new SideNavItem("Créer un joueur", CreationJoueurView.class));
}
joueurs.addItem(new SideNavItem("Liste des joueurs", ListeJoueursView.class));

        joueurs.addItem(new SideNavItem("Liste des joueurs", ListeJoueursView.class));

        /* ===== ✅ ÉQUIPES ===== */
        SideNavItem equipes = new SideNavItem("Équipes");
        if (SessionInfo.adminConnected()) {
            equipes.addItem(new SideNavItem("Gestion des équipes", GestionEquipesView.class));
        }

        /* ===== ✅ RONDES/MATCHS ===== */
        SideNavItem rondesMatchs = new SideNavItem("Rondes / Matchs");
        if (SessionInfo.adminConnected()) {
            rondesMatchs.addItem(new SideNavItem("Créer des rondes", GestionRondesMatchsView.class));
            rondesMatchs.addItem(new SideNavItem("Résultats et clôture", GestionResultatsMatchsView.class));
        }

        /* ===== TOURNOIS ===== */
        SideNavItem tournois = new SideNavItem("Tournois");
        tournois.addItem(new SideNavItem("Liste des tournois", ListeTournoisView.class));
        // ❌ SUPPRIMÉ : tournois.addItem(new SideNavItem("Classement global", ClassementGlobalView.class));
        // Le classement est maintenant accessible depuis la page d'accueil avec sélection de tournoi

        if (SessionInfo.adminConnected()) {
            tournois.addItem(new SideNavItem("Créer un tournoi", CreerTournoiView.class));
        }

        /* ===== TERRAINS ===== */
        SideNavItem terrains = new SideNavItem("Terrains");
        terrains.addItem(new SideNavItem("Liste des terrains", ListeTerrainsView.class));
        if (SessionInfo.adminConnected()) {
            terrains.addItem(new SideNavItem("Créer un terrain", CreationTerrainView.class));
            terrains.addItem(new SideNavItem("Gestion avec plan", GestionTerrainsView.class));
        }

        /* ===== ASSEMBLAGE DU MENU ===== */
        addItem(accueil, utilisateurs);

        addItem(joueurs);

        if (SessionInfo.adminConnected()) {
            addItem(equipes);
        }

        if (SessionInfo.adminConnected()) {
            addItem(rondesMatchs);
        }

        if (SessionInfo.playerConnected()) {
            addItem(new SideNavItem("Mon Espace Joueur", InterfaceJoueurView.class));
        }

        addItem(tournois);
        addItem(terrains);

        if (SessionInfo.adminConnected()) {

            SideNavItem admin = new SideNavItem("Administration");
            admin.addItem(new SideNavItem("Initialisation des données", InitialisationDonneesView.class));
            addItem(admin);
        }
    }
}