package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "equipes/gestion", layout = MainLayout.class)
@PageTitle("Gestion des équipes")
public class GestionEquipesView extends VerticalLayout {

    private Grid<EquipeInfo> grid;
    private Button rafraichirButton;

    public GestionEquipesView() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add(new Paragraph("Seuls les administrateurs peuvent gérer les équipes."));
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1200px");

        add(new H2("Gestion des équipes"));

        add(new Paragraph("Cette page affiche toutes les équipes créées pour les matchs du tournoi."));

        // Grille des équipes
        grid = new Grid<>(EquipeInfo.class, false);
        grid.addColumn(EquipeInfo::getIdEquipe).setHeader("ID Équipe").setAutoWidth(true);
        grid.addColumn(EquipeInfo::getNumeroEquipe).setHeader("N°").setAutoWidth(true);
        grid.addColumn(EquipeInfo::getIdMatch).setHeader("Match").setAutoWidth(true);
        grid.addColumn(EquipeInfo::getScore).setHeader("Score").setAutoWidth(true);
        grid.addColumn(EquipeInfo::getJoueurs).setHeader("Joueurs").setAutoWidth(true);
        
        grid.setWidthFull();
        add(grid);

        // Statistiques
        Paragraph stats = new Paragraph();
        stats.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("margin-top", "20px");
        add(stats);

        // Bouton rafraîchir
        rafraichirButton = new Button("🔄 Rafraîchir", e -> {
            try {
                chargerEquipes(stats);
            } catch (SQLException ex) {
                Notification.show("❌ Erreur : " + ex.getMessage(), 
                                5000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        rafraichirButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(rafraichirButton);

        // Chargement initial
        try {
            chargerEquipes(stats);
        } catch (SQLException ex) {
            Notification.show("❌ Erreur de chargement : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerEquipes(Paragraph stats) throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Equipe> equipes = Equipe.toutesLesEquipes(con);
            
            List<EquipeInfo> infos = new ArrayList<>();
            for (Equipe e : equipes) {
                String joueurs = e.getJoueurs().stream()
                    .map(Joueur::getSurnom)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("-");
                
                infos.add(new EquipeInfo(
                    e.getId(),
                    e.getNumero(),
                    e.getMatch() != null ? e.getMatch().getId() : -1,
                    e.getScoreTotal(),
                    joueurs
                ));
            }
            
            grid.setItems(infos);
            
            stats.setText("📊 Total : " + equipes.size() + " équipe(s) | " +
                         "Équipes complètes : " + equipes.stream().filter(Equipe::estComplete).count());
            
        } catch (SQLException ex) {
            throw ex;
        }
    }

    // Classe interne pour affichage
    public static class EquipeInfo {
        private int idEquipe;
        private int numeroEquipe;
        private int idMatch;
        private int score;
        private String joueurs;

        public EquipeInfo(int idEquipe, int numeroEquipe, int idMatch, int score, String joueurs) {
            this.idEquipe = idEquipe;
            this.numeroEquipe = numeroEquipe;
            this.idMatch = idMatch;
            this.score = score;
            this.joueurs = joueurs;
        }

        public int getIdEquipe() { return idEquipe; }
        public int getNumeroEquipe() { return numeroEquipe; }
        public int getIdMatch() { return idMatch; }
        public int getScore() { return score; }
        public String getJoueurs() { return joueurs; }
    }
}