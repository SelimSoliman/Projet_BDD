package fr.insa.toto.webui.session;

import com.vaadin.flow.server.VaadinSession;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.model.Utilisateur;
import java.util.Optional;

public class SessionInfo {

    private static final String USER_ATTRIBUTE = "user";
    private static final String TOURNOI_ACTIF_ATTRIBUTE = "tournoiActif";

    // ===== Utilisateur =====

    public static void setUserConnected(Utilisateur user) {
        VaadinSession.getCurrent().setAttribute(USER_ATTRIBUTE, user);
    }

    public static Utilisateur userConnected() {
        return (Utilisateur) VaadinSession.getCurrent().getAttribute(USER_ATTRIBUTE);
    }

    public static boolean connected() {
        return userConnected() != null;
    }

    public static boolean adminConnected() {
        Utilisateur user = userConnected();
        return user != null && user.isAdmin();
    }

    
    // ✅ AJOUT
    public static void login(Utilisateur user) {
        setUserConnected(user);
    }

    public static void logout() {
        VaadinSession.getCurrent().setAttribute(USER_ATTRIBUTE, null);
        VaadinSession.getCurrent().setAttribute(TOURNOI_ACTIF_ATTRIBUTE, null);
    }
public static Optional<Utilisateur> curUser() {
    return Optional.ofNullable(userConnected());
}
   

    // ===== Tournoi actif =====

    public static void setTournoiActif(TournoiMulti tournoi) {
        VaadinSession.getCurrent().setAttribute(TOURNOI_ACTIF_ATTRIBUTE, tournoi);
    }

    public static TournoiMulti getTournoiActif() {
        return (TournoiMulti) VaadinSession.getCurrent().getAttribute(TOURNOI_ACTIF_ATTRIBUTE);
    }

    public static boolean tournoiActifExiste() {
        return getTournoiActif() != null;
    }
    public static String getLogin() {
    Utilisateur u = userConnected();
    if (u == null) return null;

    // adapte selon ton modèle Utilisateur :
    // return u.getLogin();
    // return u.getNom();
    // return u.getPrenom();

    return u.toString(); // fallback si tu n'as pas mieux
}

    
}
