package fr.insa.toto.webui.utilisateurs;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Tournoi;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Route(value = "classement", layout = MainLayout.class)
@PageTitle("Classement")
public class ClassementView extends VerticalLayout {

    private ComboBox<TournoiInfo> tournoiCombo;
    private Grid<LigneClassement> grid;
    private Paragraph infoMessage;

    public static class LigneClassement {
        public Joueur joueur;
        public int score;
        public int rang;
        
        public LigneClassement(Joueur joueur, int score, int rang) {
            this.joueur = joueur;
            this.score = score;
            this.rang = rang;
        }
    }

    public static class TournoiInfo {
        private int id;
        private String nom;
        private boolean isTousLesTournois;

        public TournoiInfo(int id, String nom) {
            this.id = id;
            this.nom = nom;
            this.isTousLesTournois = false;
        }

        public TournoiInfo() {
            this.id = -1;
            this.nom = "🏆 Tous les tournois";
            this.isTousLesTournois = true;
        }

        public int getId() { return id; }
        public String getNom() { return nom; }
        public boolean isTousLesTournois() { return isTousLesTournois; }

        @Override
        public String toString() { return nom; }
    }

    public ClassementView() {
        setSpacing(true);
        setPadding(true);
        setMaxWidth("1200px");

        add(new H2("🏆 Classement"));

        // Info message
        infoMessage = new Paragraph();
        infoMessage.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "10px")
            .set("border-radius", "5px")
            .set("border-left", "4px solid #2196f3")
            .set("margin-bottom", "20px");
        add(infoMessage);

        // ComboBox pour sélectionner le tournoi
        tournoiCombo = new ComboBox<>("Sélectionner un tournoi");
        tournoiCombo.setItemLabelGenerator(TournoiInfo::getNom);
        tournoiCombo.setWidthFull();
        tournoiCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                chargerClassement(e.getValue());
            }
        });
        add(tournoiCombo);

        // Grille du classement
        grid = new Grid<>(LigneClassement.class, false);
        
        grid.addColumn(l -> l.rang)
            .setHeader("Rang")
            .setAutoWidth(true)
            .setFlexGrow(0);
        
        grid.addComponentColumn(l ->
            new RouterLink(
                l.joueur.getSurnom(),
                JoueurDetailView.class,
                new RouteParameters("id", String.valueOf(l.joueur.getId()))
            )
        ).setHeader("Joueur").setAutoWidth(true).setFlexGrow(1);

        grid.addColumn(l -> l.joueur.getNom() + " " + l.joueur.getPrenom())
            .setHeader("Nom complet")
            .setAutoWidth(true)
            .setFlexGrow(1);

        grid.addColumn(l -> l.score)
            .setHeader("Score Total")
            .setAutoWidth(true)
            .setFlexGrow(0);

        grid.setWidthFull();
        add(grid);

        // Charger les tournois
        try {
            chargerTournois();
        } catch (SQLException e) {
            e.printStackTrace();
            infoMessage.setText("❌ Erreur lors du chargement des tournois : " + e.getMessage());
            infoMessage.getStyle().set("background-color", "#ffebee").set("border-left", "4px solid #f44336");
        }
    }

    private void chargerTournois() throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiInfo> tournois = new ArrayList<>();
            
            // Ajouter l'option "Tous les tournois"
            tournois.add(new TournoiInfo());
            
            // Charger tous les tournois
            String sql = "SELECT id, nom FROM tournoi ORDER BY id DESC";
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tournois.add(new TournoiInfo(
                        rs.getInt("id"),
                        rs.getString("nom")
                    ));
                }
            }
            
            tournoiCombo.setItems(tournois);
            
            // Sélectionner "Tous les tournois" par défaut
            if (!tournois.isEmpty()) {
                tournoiCombo.setValue(tournois.get(0));
            }
        }
    }

    private void chargerClassement(TournoiInfo tournoiInfo) {
        try (Connection con = ConnectionPool.getConnection()) {
            List<LigneClassement> lignes;
            
            if (tournoiInfo.isTousLesTournois()) {
                // Classement global tous tournois confondus
                lignes = chargerClassementGlobal(con);
                infoMessage.setText("📊 Classement global tous tournois confondus");
            } else {
                // Classement pour un tournoi spécifique
                lignes = chargerClassementTournoi(con, tournoiInfo.getId());
                infoMessage.setText("📊 Classement du tournoi : " + tournoiInfo.getNom());
            }
            
            grid.setItems(lignes);
            
            if (lignes.isEmpty()) {
                infoMessage.setText("ℹ️ Aucun classement disponible pour ce tournoi");
                infoMessage.getStyle().set("background-color", "#fff3e0").set("border-left", "4px solid #ff9800");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            infoMessage.setText("❌ Erreur lors du chargement du classement : " + e.getMessage());
            infoMessage.getStyle().set("background-color", "#ffebee").set("border-left", "4px solid #f44336");
        }
    }

    /**
     * Classement global tous tournois confondus
     */
    private List<LigneClassement> chargerClassementGlobal(Connection con) throws SQLException {
        List<LigneClassement> lignes = new ArrayList<>();
        
        String sql = """
            SELECT 
                j.id,
                j.surnom,
                j.nom,
                j.prenom,
                j.categorie,
                j.taillecm,
                j.sexe,
                j.date_naissance,
                COALESCE(SUM(e.score), 0) as score_total
            FROM joueur j
            LEFT JOIN match_joueur mj ON j.id = mj.id_joueur
            LEFT JOIN equipe e ON mj.id_match = e.id_match AND mj.numero_equipe = e.numero
            GROUP BY j.id, j.surnom, j.nom, j.prenom, j.categorie, j.taillecm, j.sexe, j.date_naissance
            ORDER BY score_total DESC, j.surnom
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            int rang = 1;
            while (rs.next()) {
                Joueur joueur = new Joueur(
                    rs.getInt("id"),
                    rs.getString("surnom"),
                    rs.getString("categorie"),
                    rs.getInt("taillecm"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("sexe"),
                    rs.getDate("date_naissance") != null ? rs.getDate("date_naissance").toLocalDate() : null
                );
                
                int score = rs.getInt("score_total");
                lignes.add(new LigneClassement(joueur, score, rang++));
            }
        }
        
        return lignes;
    }

    /**
     * Classement pour un tournoi spécifique
     */
    private List<LigneClassement> chargerClassementTournoi(Connection con, int tournoiId) throws SQLException {
        List<LigneClassement> lignes = new ArrayList<>();
        
        String sql = """
            SELECT 
                j.id,
                j.surnom,
                j.nom,
                j.prenom,
                j.categorie,
                j.taillecm,
                j.sexe,
                j.date_naissance,
                COALESCE(SUM(e.score), 0) as score_total
            FROM joueur j
            LEFT JOIN match_joueur mj ON j.id = mj.id_joueur
            LEFT JOIN matchs m ON mj.id_match = m.id
            LEFT JOIN ronde r ON m.ronde_id = r.id
            LEFT JOIN equipe e ON mj.id_match = e.id_match AND mj.numero_equipe = e.numero
            LEFT JOIN inscription_tournoi it ON j.id = it.id_joueur AND r.id_tournoi = it.id_tournoi
            WHERE it.id_tournoi = ? OR it.id_tournoi IS NULL
            GROUP BY j.id, j.surnom, j.nom, j.prenom, j.categorie, j.taillecm, j.sexe, j.date_naissance
            ORDER BY score_total DESC, j.surnom
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            
            try (ResultSet rs = ps.executeQuery()) {
                int rang = 1;
                while (rs.next()) {
                    Joueur joueur = new Joueur(
                        rs.getInt("id"),
                        rs.getString("surnom"),
                        rs.getString("categorie"),
                        rs.getInt("taillecm"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("sexe"),
                        rs.getDate("date_naissance") != null ? rs.getDate("date_naissance").toLocalDate() : null
                    );
                    
                    int score = rs.getInt("score_total");
                    lignes.add(new LigneClassement(joueur, score, rang++));
                }
            }
        }
        
        return lignes;
    }
}