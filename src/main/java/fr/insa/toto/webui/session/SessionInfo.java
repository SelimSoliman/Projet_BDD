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

    // ✅ AJOUT (tu l'avais déjà, je garde)
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

    // ===== ✅ JOUEUR (ce qu’il te manquait) =====

    /** True si connecté et rôle joueur */
    public static boolean playerConnected() {
        Utilisateur user = userConnected();
        return user != null && user.isPlayer();
    }

    /** id du joueur lié au compte (null si pas un joueur) */
    public static Integer getIdJoueurConnecte() {
        Utilisateur user = userConnected();
        return user != null ? user.getIdJoueur() : null;
    }

    /** login affichable (chez toi c’est le surnom) */
    public static String getLogin() {
        Utilisateur u = userConnected();
        return (u == null) ? null : u.getSurnom();
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
}
