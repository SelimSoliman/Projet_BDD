package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.utilisateurs.ClassementView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Teqball")
public class VuePrincipale extends VerticalLayout {

    public VuePrincipale() {
        // page sans padding Vaadin
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        // Container centré
        Div container = new Div();
        container.addClassName("container");

        // HERO (carte)
        Div hero = new Div();
        hero.addClassName("hero");

        H1 title = new H1("Bienvenue au site officiel du tournoi de Teqball");
        Paragraph p = new Paragraph(
                "Bienvenue sur la plateforme officielle du tournoi de Teqball. " +
                "Suivez l'évolution des matchs en temps réel, découvrez les équipes, " +
                "consultez les classements et vivez chaque ronde du tournoi au plus près. " +
                "Les organisateurs disposent d'outils dédiés pour gérer les joueurs, " +
                "les terrains et les résultats en toute simplicité. " +
                "Une expérience claire, rapide et pensée pour le jeu."
        );

        Button cta = new Button("Voir le classement");
        cta.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cta.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(ClassementView.class)));

        hero.add(title, p, cta);

        // STATS - Récupération depuis la BDD
        int nbJoueurs = 0;
        int nbRondes = 0;
        int nbTournois = 0;

        try (Connection con = ConnectionPool.getConnection()) {
            // Nombre de joueurs
            nbJoueurs = Joueur.count(con);

            // Nombre de tournois
            nbTournois = compterTournois(con);

            // Nombre de rondes (tournoi unique)
            Tournoi tournoi = Tournoi.getTournoiUnique(con);
            if (tournoi != null) {
                nbRondes = compterRondes(con, tournoi.getId());
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            // En cas d'erreur, on garde les valeurs à 0
        }

        Div stats = new Div();
        stats.addClassName("stats");
        stats.add(statCard(String.valueOf(nbJoueurs), "Joueurs"));
        stats.add(statCard(String.valueOf(nbRondes), "Rondes"));
        stats.add(statCard(String.valueOf(nbTournois), "Tournois"));

        container.add(hero, stats);
        add(container);
    }

    /**
     * Compte le nombre total de rondes pour un tournoi
     */
    private int compterRondes(Connection con, int tournoiId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ronde WHERE id_tournoi = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Compte le nombre de tournois dans toute la base
     */
    private int compterTournois(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tournoi";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Div statCard(String value, String label) {
        Div card = new Div();
        card.addClassName("stat-card");

        Div v = new Div();
        v.addClassName("stat-value");
        v.setText(value);

        Div l = new Div();
        l.addClassName("stat-label");
        l.setText(label);

        card.add(v, l);
        return card;
    }
}