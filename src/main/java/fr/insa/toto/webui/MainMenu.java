package fr.insa.toto.webui;

import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import fr.insa.toto.webui.session.SessionInfo;
import fr.insa.toto.webui.utilisateurs.CreationAdmin;
import fr.insa.toto.webui.utilisateurs.ListeUtilisateurs;
import fr.insa.toto.webui.joueurs.InterfaceJoueurView;
import fr.insa.toto.webui.extensions.*;
import fr.insa.toto.webui.tournois.CreerTournoiView;
// ⚠️ IMPORTANT : mets ici le BON import de NewRonde selon ton projet
// Si ta classe est dans fr.insa.toto.webui.NewRonde, utilise :
import fr.insa.toto.webui.utilisateurs.NewRonde;
// (et supprime l'import fr.insa.toto.webui.utilisateurs.NewRonde si tu l'avais)

public class MainMenu extends SideNav {

    public MainMenu() {

        // ACCUEIL
        SideNavItem accueil = new SideNavItem("Accueil", VuePrincipale.class);

        // UTILISATEURS
        SideNavItem utilisateurs = new SideNavItem("Utilisateurs");
        utilisateurs.addItem(new SideNavItem("Liste", ListeUtilisateurs.class));
        if (SessionInfo.adminConnected()) {
            utilisateurs.addItem(new SideNavItem("Création", CreationAdmin.class));
        }

        // TOURNOIS
        SideNavItem tournois = new SideNavItem("Tournois");
tournois.addItem(new SideNavItem("Liste des tournois", ListeTournoisView.class));
tournois.addItem(new SideNavItem("Classement global", ClassementGlobalView.class));

if (SessionInfo.adminConnected()) {
    tournois.addItem(new SideNavItem("Créer un tournoi", CreerTournoiView.class));
}


        // TERRAINS
        SideNavItem terrains = new SideNavItem("Terrains");
        if (SessionInfo.adminConnected()) {
            terrains.addItem(new SideNavItem("Gestion avec plan", GestionTerrainsView.class));
        }

        // On assemble SANS NULL
        this.addItem(accueil, utilisateurs);

        if (SessionInfo.userConnected()) {
            this.addItem(new SideNavItem("Mon Espace Joueur", InterfaceJoueurView.class));
        }

        this.addItem(tournois);

        if (SessionInfo.adminConnected()) {
            this.addItem(terrains);

            SideNavItem typesJeu = new SideNavItem("Types de Jeu");
            typesJeu.addItem(new SideNavItem("Gestion", GestionTypesJeuView.class));
            this.addItem(typesJeu);

            SideNavItem templates = new SideNavItem("Templates");
            templates.addItem(new SideNavItem("Gestion des templates", GestionTemplatesView.class));
            this.addItem(templates);
        }
    }
}
