package fr.insa.toto.webui;

import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import fr.insa.toto.webui.session.SessionInfo;

// vues utilisateurs
import fr.insa.toto.webui.utilisateurs.CreationAdmin;
import fr.insa.toto.webui.utilisateurs.ListeUtilisateurs;

// ✅ vues joueurs (AJOUT)
import fr.insa.toto.webui.joueurs.CreationJoueurView;
import fr.insa.toto.webui.joueurs.ListeJoueursView;

// ✅ vues équipes (AJOUT)
import fr.insa.toto.webui.extensions.GestionEquipesView;

// ✅ vues rondes/matchs (AJOUT)
import fr.insa.toto.webui.extensions.GestionRondesMatchsView;

// ✅ vue initialisation données (AJOUT)
import fr.insa.toto.webui.extensions.InitialisationDonneesView;

// vues joueur espace perso
import fr.insa.toto.webui.joueurs.InterfaceJoueurView;

// vues tournois / extensions
import fr.insa.toto.webui.extensions.ListeTournoisView;
import fr.insa.toto.webui.extensions.ClassementGlobalView;
import fr.insa.toto.webui.extensions.GestionTerrainsView;
import fr.insa.toto.webui.extensions.CreationTerrainView;
import fr.insa.toto.webui.extensions.ListeTerrainsView;
import fr.insa.toto.webui.extensions.GestionTypesJeuView;
import fr.insa.toto.webui.extensions.GestionTemplatesView;

// autres vues
import fr.insa.toto.webui.tournois.CreerTournoiView;

public class MainMenu extends SideNav {

    public MainMenu() {
        addClassName("sidebar"); // ✅ important pour ton CSS

        /* ===== ACCUEIL ===== */
        SideNavItem accueil = new SideNavItem("Accueil", VuePrincipale.class);

        /* ===== UTILISATEURS ===== */
        SideNavItem utilisateurs = new SideNavItem("Utilisateurs");
        utilisateurs.addItem(new SideNavItem("Liste", ListeUtilisateurs.class));

        if (SessionInfo.adminConnected()) {
            utilisateurs.addItem(new SideNavItem("Création", CreationAdmin.class));
        }

        /* ===== ✅ JOUEURS (NOUVEAU) ===== */
        SideNavItem joueurs = new SideNavItem("Joueurs");
        joueurs.addItem(new SideNavItem("Créer un joueur", CreationJoueurView.class));
        joueurs.addItem(new SideNavItem("Liste des joueurs", ListeJoueursView.class));

        /* ===== ✅ ÉQUIPES (NOUVEAU) ===== */
        SideNavItem equipes = new SideNavItem("Équipes");
        if (SessionInfo.adminConnected()) {
            equipes.addItem(new SideNavItem("Gestion des équipes", GestionEquipesView.class));
        }

        /* ===== ✅ RONDES/MATCHS (NOUVEAU) ===== */
        SideNavItem rondesMatchs = new SideNavItem("Rondes / Matchs");
        if (SessionInfo.adminConnected()) {
            rondesMatchs.addItem(new SideNavItem("Gestion rondes et matchs", GestionRondesMatchsView.class));
        }

        /* ===== TOURNOIS ===== */
        SideNavItem tournois = new SideNavItem("Tournois");
        tournois.addItem(new SideNavItem("Liste des tournois", ListeTournoisView.class));
        tournois.addItem(new SideNavItem("Classement global", ClassementGlobalView.class));

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

        // ✅ AJOUT de la section Joueurs
        addItem(joueurs);

        // ✅ AJOUT de la section Équipes
        if (SessionInfo.adminConnected()) {
            addItem(equipes);
        }

        // ✅ AJOUT de la section Rondes/Matchs
        if (SessionInfo.adminConnected()) {
            addItem(rondesMatchs);
        }

        if (SessionInfo.connected()) {
            addItem(new SideNavItem("Mon Espace Joueur", InterfaceJoueurView.class));
        }

        addItem(tournois);
        addItem(terrains);

        if (SessionInfo.adminConnected()) {

            SideNavItem typesJeu = new SideNavItem("Types de Jeu");
            typesJeu.addItem(new SideNavItem("Gestion", GestionTypesJeuView.class));
            addItem(typesJeu);

            SideNavItem templates = new SideNavItem("Templates");
            templates.addItem(new SideNavItem("Gestion des templates", GestionTemplatesView.class));
            addItem(templates);

            SideNavItem admin = new SideNavItem("Administration");
            admin.addItem(new SideNavItem("Initialisation des données", InitialisationDonneesView.class));
            addItem(admin);
        }
    }
}