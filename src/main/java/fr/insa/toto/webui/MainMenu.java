package fr.insa.toto.webui;

import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import fr.insa.toto.webui.session.SessionInfo;
import fr.insa.toto.webui.utilisateurs.CreationAdmin;
import fr.insa.toto.webui.utilisateurs.ListeUtilisateurs;
import fr.insa.toto.webui.joueurs.InterfaceJoueurView;
import fr.insa.toto.webui.extensions.*;

/**
 * Menu principal avec toutes les extensions intégrées
 */
public class MainMenu extends SideNav {

    public MainMenu() {

        // ===== ACCUEIL =====
        SideNavItem accueil = new SideNavItem("Accueil", VuePrincipale.class);

        // ===== UTILISATEURS =====
        SideNavItem utilisateurs = new SideNavItem("Utilisateurs");
        utilisateurs.addItem(new SideNavItem("Liste", ListeUtilisateurs.class));

        if (SessionInfo.adminConnected()) {
            utilisateurs.addItem(new SideNavItem("Création", CreationAdmin.class));
        }

        // ===== MON ESPACE JOUEUR (Extension 1) =====
        SideNavItem monEspace = null;
        if (SessionInfo.userConnected()) {
            monEspace = new SideNavItem("Mon Espace Joueur", InterfaceJoueurView.class);
        }

        // ===== TOURNOIS =====
        SideNavItem tournois = new SideNavItem("Tournois");
        
        // Extension 2 : Multi-tournoi
        tournois.addItem(new SideNavItem("Liste des tournois", ListeTournoisView.class));
        tournois.addItem(new SideNavItem("Classement global", ClassementGlobalView.class));

        // Admin only
        if (SessionInfo.adminConnected()) {
            tournois.addItem(new SideNavItem("Paramètres", TournoiParamView.class));
            tournois.addItem(new SideNavItem("Créer une ronde", NewRonde.class));
            tournois.addItem(new SideNavItem("Résultat match", MatchResultView.class));
            tournois.addItem(new SideNavItem("Créer un match", MatchCreateView.class));
        }

        // ===== TERRAINS (Extension 3) =====
        SideNavItem terrains = new SideNavItem("Terrains");
        
        if (SessionInfo.adminConnected()) {
            terrains.addItem(new SideNavItem("Gestion avec plan", GestionTerrainsView.class));
        }

        // ===== TYPES DE JEU (Extensions 4-5) =====
        SideNavItem typesJeu = null;
        if (SessionInfo.adminConnected()) {
            typesJeu = new SideNavItem("Types de Jeu");
            typesJeu.addItem(new SideNavItem("Gestion", GestionTypesJeuView.class));
        }

        // ===== TEMPLATES (Extension 6) =====
        SideNavItem templates = null;
        if (SessionInfo.adminConnected()) {
            templates = new SideNavItem("Templates");
            templates.addItem(new SideNavItem("Gestion des templates", GestionTemplatesView.class));
        }

        // ===== ASSEMBLAGE DU MENU =====
        if (SessionInfo.adminConnected()) {
            // Menu complet pour admin
            this.addItem(
                accueil,
                utilisateurs,
                monEspace,
                tournois,
                terrains,
                typesJeu,
                templates
            );
        } else if (SessionInfo.userConnected()) {
            // Menu pour utilisateur simple
            this.addItem(
                accueil,
                utilisateurs,
                monEspace,
                tournois  // Consultation uniquement
            );
        } else {
            // Menu minimal pour visiteur
            this.addItem(accueil, utilisateurs);
        }
    }
}