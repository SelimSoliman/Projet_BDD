package fr.insa.toto.webui.joueurs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Vue détaillée d'un joueur avec la liste de ses matchs
 */
@Route(value = "joueurs/detail", layout = MainLayout.class)
@PageTitle("Détails du joueur")
public class JoueurDetailView extends VerticalLayout implements HasUrlParameter<Integer> {

    private int joueurId;
    private Joueur joueur;
    private VerticalLayout infoSection;
    private Grid<MatchInfo> gridMatchs;

    public JoueurDetailView() {
        setPadding(true);
        setSpacing(true);
        
        // Bouton retour
        Button retourButton = new Button("← Retour à la liste", event -> {
            getUI().ifPresent(ui -> ui.navigate("joueurs/liste"));
        });
        retourButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        add(retourButton);
    }

    @Override
    public void setParameter(BeforeEvent event, Integer parameter) {
        this.joueurId = parameter;
        chargerDonnees();
    }

    private void chargerDonnees() {
        try (Connection con = ConnectionPool.getConnection()) {
            // Charger les infos du joueur
            joueur = chargerJoueur(con, joueurId);
            
            if (joueur == null) {
                Notification.show("❌ Joueur non trouvé", 3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_ERROR);
                getUI().ifPresent(ui -> ui.navigate("joueurs/liste"));
                return;
            }

            // Afficher les informations du joueur
            afficherInfosJoueur();
            
            // Charger et afficher les matchs
            List<MatchInfo> matchs = chargerMatchsJoueur(con, joueurId);
            afficherMatchs(matchs);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur : " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void afficherInfosJoueur() {
        // Titre avec le nom du joueur
        H2 titre = new H2("Profil de " + joueur.getSurnom());
        add(titre);

        // Section des informations personnelles
        infoSection = new VerticalLayout();
        infoSection.setPadding(true);
        infoSection.getStyle()
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px")
            .set("margin-bottom", "20px");

        // Informations détaillées
        addInfoRow("👤 Nom complet", joueur.getNom() + " " + joueur.getPrenom());
        addInfoRow("🏷️ Surnom", joueur.getSurnom());
        addInfoRow("⚥ Sexe", joueur.getSexe());
        addInfoRow("📏 Taille", joueur.getTaillecm() + " cm");
        addInfoRow("🏆 Catégorie", joueur.getCategorie());
        
        // Calculer l'âge
        LocalDate dateNaissance = joueur.getDateNaissance();
        if (dateNaissance != null) {
            int age = Period.between(dateNaissance, LocalDate.now()).getYears();
            addInfoRow("🎂 Date de naissance", dateNaissance.toString() + " (" + age + " ans)");
        }

        add(infoSection);
    }

    private void addInfoRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(Alignment.BASELINE);
        row.setWidthFull();

        Paragraph labelPara = new Paragraph(label);
        labelPara.getStyle()
            .set("font-weight", "bold")
            .set("margin", "5px 0")
            .set("min-width", "200px");

        Paragraph valuePara = new Paragraph(value);
        valuePara.getStyle().set("margin", "5px 0");

        row.add(labelPara, valuePara);
        infoSection.add(row);
    }

    private void afficherMatchs(List<MatchInfo> matchs) {
        H3 titreMatchs = new H3("📋 Historique des matchs");
        add(titreMatchs);

        if (matchs.isEmpty()) {
            Paragraph aucunMatch = new Paragraph("Aucun match joué pour le moment.");
            aucunMatch.getStyle()
                .set("color", "#666")
                .set("font-style", "italic")
                .set("padding", "20px")
                .set("text-align", "center");
            add(aucunMatch);
            return;
        }

        // Statistiques des matchs
        Paragraph stats = new Paragraph("📊 Total : " + matchs.size() + " match(s)");
        stats.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "10px")
            .set("border-radius", "5px")
            .set("margin-bottom", "10px");
        add(stats);

        // Grille des matchs
        gridMatchs = new Grid<>(MatchInfo.class, false);
        
        gridMatchs.addColumn(MatchInfo::getRonde)
            .setHeader("Ronde")
            .setAutoWidth(true)
            .setSortable(true);

        gridMatchs.addColumn(MatchInfo::getNumeroMatch)
            .setHeader("Match N°")
            .setAutoWidth(true)
            .setSortable(true);

        gridMatchs.addColumn(MatchInfo::getTournoi)
            .setHeader("Tournoi")
            .setAutoWidth(true)
            .setSortable(true);

        gridMatchs.addColumn(match -> {
            String statut = match.getStatut();
            return statut != null ? statut : "En attente";
        })
            .setHeader("Statut")
            .setAutoWidth(true)
            .setSortable(true);

        gridMatchs.addColumn(match -> {
            Integer score = match.getScore();
            return score != null ? score.toString() : "-";
        })
            .setHeader("Score")
            .setAutoWidth(true)
            .setSortable(true);

        gridMatchs.addColumn(MatchInfo::getDateRonde)
            .setHeader("Date")
            .setAutoWidth(true)
            .setSortable(true);

        gridMatchs.setItems(matchs);
        gridMatchs.setHeight("auto");
        
        add(gridMatchs);

        // Aide
        Paragraph aide = new Paragraph("💡 Cliquez sur les en-têtes pour trier les colonnes");
        aide.getStyle()
            .set("color", "#666")
            .set("font-style", "italic")
            .set("margin-top", "10px");
        add(aide);
    }

    /**
     * Charge les informations d'un joueur depuis la base de données
     */
    private Joueur chargerJoueur(Connection con, int joueurId) throws SQLException {
        String sql = """
            SELECT id, surnom, categorie, taillecm, nom, prenom, sexe, date_naissance
            FROM joueur
            WHERE id = ?
            """;
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, joueurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Joueur.fromResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Charge tous les matchs auxquels a participé un joueur
     */
    private List<MatchInfo> chargerMatchsJoueur(Connection con, int joueurId) throws SQLException {
    List<MatchInfo> matchs = new ArrayList<>();
    
    String sql = """
        SELECT 
            m.id as match_id,
            r.numero as ronde_numero,
            r.debut as ronde_date,
            t.nom as tournoi_nom,
            m.statut as match_statut,
            e.score as joueur_score
        FROM match_joueur mj
        JOIN matchs m ON mj.id_match = m.id
        JOIN ronde r ON m.ronde_id = r.id
        JOIN tournoi t ON r.id_tournoi = t.id
        LEFT JOIN equipe e ON e.id_match = m.id AND e.numero = mj.numero_equipe
        WHERE mj.id_joueur = ?
        ORDER BY r.debut DESC, r.numero DESC, m.id
        """;
    
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, joueurId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MatchInfo info = new MatchInfo(
                    rs.getInt("match_id"),
                    rs.getInt("ronde_numero"),
                    rs.getString("tournoi_nom"),
                    rs.getString("match_statut"),
                    rs.getObject("joueur_score", Integer.class),
                    rs.getDate("ronde_date") != null ? 
                        rs.getDate("ronde_date").toLocalDate().toString() : "-"
                );
                matchs.add(info);
            }
        }
    }
    
    return matchs;
}

    /**
     * Classe interne pour stocker les informations d'un match
     */
    public static class MatchInfo {
        private int matchId;
        private int rondeNumero;
        private String tournoi;
        private String statut;
        private Integer score;
        private String dateRonde;

        public MatchInfo(int matchId, int rondeNumero, String tournoi, 
                        String statut, Integer score, String dateRonde) {
            this.matchId = matchId;
            this.rondeNumero = rondeNumero;
            this.tournoi = tournoi;
            this.statut = statut;
            this.score = score;
            this.dateRonde = dateRonde;
        }

        public int getMatchId() { return matchId; }
        public String getRonde() { return "Ronde " + rondeNumero; }
        public int getNumeroMatch() { return matchId; }
        public String getTournoi() { return tournoi; }
        public String getStatut() { return statut; }
        public Integer getScore() { return score; }
        public String getDateRonde() { return dateRonde; }
    }
}