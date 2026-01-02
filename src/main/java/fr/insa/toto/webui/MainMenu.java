package fr.insa.toto.webui;

import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import fr.insa.toto.webui.session.SessionInfo;
import fr.insa.toto.webui.utilisateurs.CreationAdmin;
import fr.insa.toto.webui.utilisateurs.ListeUtilisateurs;

public class MainMenu extends SideNav {

    public MainMenu() {

        SideNavItem accueil = new SideNavItem("Accueil", VuePrincipale.class);

        SideNavItem utilisateurs = new SideNavItem("Utilisateurs");
        utilisateurs.addItem(new SideNavItem("Liste", ListeUtilisateurs.class));

        if (SessionInfo.adminConnected()) {
            utilisateurs.addItem(new SideNavItem("Création", CreationAdmin.class));
        }

        if (SessionInfo.adminConnected()) {
            SideNavItem tournoiMenu = new SideNavItem("Tournoi");

            tournoiMenu.addItem(new SideNavItem("Paramètres du tournoi", TournoiParamView.class));
            tournoiMenu.addItem(new SideNavItem("Créer une ronde", NewRonde.class));
            tournoiMenu.addItem(new SideNavItem("Saisir résultat match", MatchResultView.class)); // ✅ ici

            this.addItem(accueil, utilisateurs, tournoiMenu);
        } else {
            this.addItem(accueil, utilisateurs);
        }
    }
}
