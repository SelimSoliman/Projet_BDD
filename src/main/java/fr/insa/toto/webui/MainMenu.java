package fr.insa.toto.webui;

import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.icon.VaadinIcon;



import fr.insa.toto.webui.session.SessionInfo;

// vues utilisateurs
import fr.insa.toto.webui.utilisateurs.CreationAdmin;
import fr.insa.toto.webui.utilisateurs.ListeUtilisateurs;

// vues joueur
import fr.insa.toto.webui.joueurs.InterfaceJoueurView;

// vues tournois / extensions
import fr.insa.toto.webui.extensions.ListeTournoisView;
import fr.insa.toto.webui.extensions.ClassementGlobalView;
import fr.insa.toto.webui.extensions.GestionTerrainsView;
import fr.insa.toto.webui.extensions.GestionTypesJeuView;
import fr.insa.toto.webui.extensions.GestionTemplatesView;

// autres vues
import fr.insa.toto.webui.tournois.CreerTournoiView;
import fr.insa.toto.webui.utilisateurs.NewRonde;

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

        /* ===== TOURNOIS ===== */
        SideNavItem tournois = new SideNavItem("Tournois");
        tournois.addItem(new SideNavItem("Liste des tournois", ListeTournoisView.class));
        tournois.addItem(new SideNavItem("Classement global", ClassementGlobalView.class));

        if (SessionInfo.adminConnected()) {
            tournois.addItem(new SideNavItem("Créer un tournoi", CreerTournoiView.class));
            tournois.addItem(new SideNavItem("Créer une ronde", NewRonde.class));
        }

        /* ===== TERRAINS ===== */
        SideNavItem terrains = new SideNavItem("Terrains");
        if (SessionInfo.adminConnected()) {
            terrains.addItem(new SideNavItem("Gestion avec plan", GestionTerrainsView.class));
        }

        /* ===== ASSEMBLAGE DU MENU ===== */
        addItem(accueil, utilisateurs);

        if (SessionInfo.connected()) {
            addItem(new SideNavItem("Mon Espace Joueur", InterfaceJoueurView.class));
        }

        addItem(tournois);

        if (SessionInfo.adminConnected()) {
            addItem(terrains);

            SideNavItem typesJeu = new SideNavItem("Types de Jeu");
            typesJeu.addItem(new SideNavItem("Gestion", GestionTypesJeuView.class));
            addItem(typesJeu);

            SideNavItem templates = new SideNavItem("Templates");
            templates.addItem(new SideNavItem("Gestion des templates", GestionTemplatesView.class));
            addItem(templates);
        }
    }

}
