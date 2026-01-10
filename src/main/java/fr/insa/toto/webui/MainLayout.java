package fr.insa.toto.webui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import fr.insa.toto.model.Utilisateur;
import fr.insa.toto.webui.session.SessionInfo;

public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);

        // Topbar (pro)
        addToNavbar(true, buildTopbar());

        // Drawer seulement si connecté
        if (SessionInfo.connected()) {
            addToDrawer(new MainMenu());
        }
    }

    private Component buildTopbar() {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("Tournoi de Teqball");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "700");

        Component userPart = buildUserMenu();

        HorizontalLayout bar = new HorizontalLayout(toggle, title, userPart);
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.expand(title);
        bar.addClassName("topbar");

        return bar;
    }

    private Component buildUserMenu() {
        if (!SessionInfo.connected()) {
            return new HorizontalLayout();
        }

       String username = SessionInfo.curUser()
        .map(Utilisateur::getSurnom)
        .orElse("Compte");

        MenuBar menu = new MenuBar();
        menu.addClassName("user-menu");
        menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        var user = menu.addItem(username != null ? username : "Compte");
        user.getSubMenu().addItem("Mon compte");
        user.getSubMenu().addItem("Déconnexion", e -> {
            SessionInfo.logout();
            getUI().ifPresent(ui -> ui.navigate("login"));
        });

        return menu;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean isLoginPage = event.getLocation().getPath().equals("login");

        if (!SessionInfo.connected() && !isLoginPage) {
            event.forwardTo("login");
        }
    }
}
