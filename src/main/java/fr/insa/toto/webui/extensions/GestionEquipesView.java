package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

        add(new H2("👥 Gestion des équipes"));

        Paragraph info = new Paragraph();
        info.setText("💡 Cette page affiche toutes les équipes créées pour les matchs. " +
                    "Les équipes sont générées automatiquement lors de la création des rondes.");
        info.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("border-left", "4px solid #2196f3");
        add(info);

        // Grille des équipes
        grid = new Grid<>(EquipeInfo.class, false);
        grid.addColumn(EquipeInfo::getIdEquipe).setHeader("ID Équipe").setAutoWidth(true);
        grid.addColumn(EquipeInfo::getNumeroEquipe).setHeader("N° Équipe").setAutoWidth(true);
        grid.addColumn(EquipeInfo::getInfo).setHeader("Informations").setAutoWidth(true).setFlexGrow(1);
        
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
                try {
                    StringBuilder info = new StringBuilder();
                    
                    // Joueurs
                    List<Joueur> joueurs = e.getJoueurs();
                    if (!joueurs.isEmpty()) {
                        info.append("Joueurs: ");
                        for (int i = 0; i < joueurs.size(); i++) {
                            if (i > 0) info.append(", ");
                            info.append(joueurs.get(i).getSurnom());
                        }
                    } else {
                        info.append("Aucun joueur");
                    }
                    
                    // Match
                    if (e.getMatch() != null) {
                        info.append(" | Match: #").append(e.getMatch().getId());
                    }
                    
                    infos.add(new EquipeInfo(
                        e.getId(),
                        e.getNumero(),
                        info.toString()
                    ));
                    
                } catch (Exception ex) {
                    // Ignorer les équipes qui causent des erreurs
                    System.err.println("Erreur lors du chargement de l'équipe " + e.getId() + ": " + ex.getMessage());
                }
            }
            
            grid.setItems(infos);
            
            stats.setText("📊 Total : " + infos.size() + " équipe(s)");
            
        } catch (SQLException ex) {
            throw ex;
        }
    }

    // Classe interne pour affichage
    public static class EquipeInfo {
        private int idEquipe;
        private int numeroEquipe;
        private String info;

        public EquipeInfo(int idEquipe, int numeroEquipe, String info) {
            this.idEquipe = idEquipe;
            this.numeroEquipe = numeroEquipe;
            this.info = info;
        }

        public int getIdEquipe() { return idEquipe; }
        public int getNumeroEquipe() { return numeroEquipe; }
        public String getInfo() { return info; }
    }
}