package fr.insa.toto.webui.session;

import com.vaadin.flow.server.VaadinSession;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.model.Utilisateur;

/**
 * Classe pour gérer les informations de session (utilisateur connecté, tournoi actif).
 */
public class SessionInfo {

    private static final String USER_ATTRIBUTE = "user";
    private static final String TOURNOI_ACTIF_ATTRIBUTE = "tournoiActif";

    // ========== GESTION UTILISATEUR ==========

    public static void setUserConnected(Utilisateur user) {
        VaadinSession.getCurrent().setAttribute(USER_ATTRIBUTE, user);
    }

    public static Utilisateur userConnected() {
        return VaadinSession.getCurrent().getAttribute(Utilisateur.class);
    }

    public static boolean connected() {
        return userConnected() != null;
    }

    public static boolean adminConnected() {
        Utilisateur user = userConnected();
        return user != null && user.isAdmin();
    }

    public static void logout() {
        VaadinSession.getCurrent().setAttribute(USER_ATTRIBUTE, null);
        VaadinSession.getCurrent().setAttribute(TOURNOI_ACTIF_ATTRIBUTE, null);
    }

    // ========== GESTION TOURNOI ACTIF ==========

    /**
     * Définit le tournoi actif pour la session en cours.
     * @param tournoi Le tournoi à définir comme actif (ou null pour désélectionner)
     */
    public static void setTournoiActif(TournoiMulti tournoi) {
        VaadinSession.getCurrent().setAttribute(TOURNOI_ACTIF_ATTRIBUTE, tournoi);
    }

    /**
     * Récupère le tournoi actuellement actif.
     * @return Le tournoi actif, ou null si aucun n'est sélectionné
     */
    public static TournoiMulti getTournoiActif() {
        return VaadinSession.getCurrent().getAttribute(TournoiMulti.class);
    }

    /**
     * Vérifie si un tournoi est actuellement actif.
     * @return true si un tournoi est actif, false sinon
     */
    public static boolean tournoiActifExiste() {
        return getTournoiActif() != null;
    }
}