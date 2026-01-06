package fr.insa.toto.webui.utilisateurs;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.webui.utilisateurs.JoueurDetailView;
import com.vaadin.flow.router.RouteParameters;
import fr.insa.toto.webui.MainLayout;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Route(value = "classement", layout=MainLayout.class)
@PageTitle("Classement")
public class ClassementView extends VerticalLayout {

    public static class LigneClassement {
        public Joueur joueur;
        public int score;
        public LigneClassement(Joueur joueur, int score) { this.joueur = joueur; this.score = score; }
    }

    public ClassementView() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Classement"));

        Grid<LigneClassement> grid = new Grid<>(LigneClassement.class, false);
        grid.addComponentColumn(l ->
        new RouterLink(
                l.joueur.getSurnom(),
                JoueurDetailView.class,
                new RouteParameters("id", String.valueOf(l.joueur.getId()))
        )
).setHeader("Joueur").setAutoWidth(true);

        grid.addColumn(l -> l.score).setHeader("Score").setAutoWidth(true);

        add(grid);

        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            Tournoi t = Tournoi.getUnique(con);
            if (t == null) return;

            // charger joueurs en mémoire (sinon classementJoueurs() trie une liste vide)
            t.clearJoueurs();
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            for (Joueur j : joueurs) t.ajouterJoueur(j);

            List<Joueur> ordre = t.classementJoueurs(con);
            List<LigneClassement> lignes = ordre.stream()
                    .map(j -> {
                        try { return new LigneClassement(j, t.computeScore(j, con)); }
                        catch (SQLException e) { return new LigneClassement(j, 0); }
                    }).toList();

            grid.setItems(lignes);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
