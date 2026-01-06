package fr.insa.toto.webui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import fr.insa.toto.webui.session.LogoutEntete;
import fr.insa.toto.webui.session.SessionInfo;

public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public MainLayout() {
        // Toujours afficher le toggle (au moins pour ne pas crasher)
        this.addToNavbar(new DrawerToggle());

        // Si connecté, on essaie de construire le menu
        if (SessionInfo.userConnected()) {
            try {
                this.addToDrawer(new MainMenu());
            } catch (Exception e) {
                System.err.println("ERREUR lors de new MainMenu() dans MainLayout");
                e.printStackTrace(); // <-- tu verras l'erreur exacte dans la console
            }

            try {
                this.addToNavbar(new LogoutEntete());
            } catch (Exception e) {
                System.err.println("ERREUR lors de new LogoutEntete() dans MainLayout");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean isLoginPage = event.getLocation().getPath().equals("login");

        if (!SessionInfo.userConnected() && !isLoginPage) {
            event.forwardTo("login");
        }
    }
}
