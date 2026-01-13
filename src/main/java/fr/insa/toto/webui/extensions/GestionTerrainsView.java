package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.MainLayout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "terrains/gestion-plan", layout = MainLayout.class)
@PageTitle("Gestion avec plan")
public class GestionTerrainsView extends VerticalLayout {

    private Div planContainer;
    private Paragraph infoBox;

    public GestionTerrainsView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("🏟️ Plan des terrains - Vue interactive"));

        infoBox = new Paragraph("Cliquez sur un terrain pour voir les matchs en cours");
        infoBox.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "12px")
            .set("border-radius", "6px")
            .set("border-left", "4px solid #2196f3")
            .set("margin-bottom", "20px");
        add(infoBox);

        Button refreshButton = new Button("🔄 Rafraîchir", e -> chargerPlan());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        add(refreshButton);

        planContainer = new Div();
        planContainer.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fill, minmax(220px, 1fr))")
            .set("gap", "20px")
            .set("padding", "20px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px")
            .set("min-height", "300px");
        
        add(planContainer);

        chargerPlan();
    }

    private void chargerPlan() {
        planContainer.removeAll();

        try (Connection con = ConnectionPool.getConnection()) {
            List<Terrain> terrains = Terrain.tousLesTerrains(con);

            if (terrains.isEmpty()) {
                Paragraph empty = new Paragraph("Aucun terrain créé. Créez des terrains d'abord !");
                empty.getStyle()
                    .set("text-align", "center")
                    .set("color", "#666")
                    .set("padding", "40px");
                planContainer.add(empty);
                return;
            }

            for (Terrain terrain : terrains) {
                planContainer.add(creerCarteTerrain(con, terrain));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Div creerCarteTerrain(Connection con, Terrain terrain) throws SQLException {
        Div carte = new Div();
        
        // Récupérer le match en cours sur ce terrain
        MatchInfo matchEnCours = getMatchEnCours(con, terrain.getId());
        
        boolean occupe = matchEnCours != null;
        
        carte.getStyle()
            .set("background-color", occupe ? "#fff3cd" : "#d4edda")
            .set("border", occupe ? "3px solid #ff9800" : "3px solid #28a745")
            .set("border-radius", "8px")
            .set("padding", "15px")
            .set("cursor", "pointer")
            .set("transition", "transform 0.2s, box-shadow 0.2s")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        carte.addClickListener(e -> {
            if (occupe) {
                afficherMatchDialog(matchEnCours);
            } else {
                Notification.show("🟢 Terrain libre : " + terrain.getNom(), 
                                2000, Notification.Position.MIDDLE);
            }
        });

        // Effet hover
        carte.getElement().addEventListener("mouseenter", ev -> {
            carte.getStyle()
                .set("transform", "translateY(-5px)")
                .set("box-shadow", "0 6px 12px rgba(0,0,0,0.15)");
        });
        
        carte.getElement().addEventListener("mouseleave", ev -> {
            carte.getStyle()
                .set("transform", "translateY(0)")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");
        });

        // Contenu de la carte
        H3 titre = new H3("🏟️ " + terrain.getNom());
        titre.getStyle()
            .set("margin", "0 0 10px 0")
            .set("font-size", "18px")
            .set("color", "#333");
        
        Paragraph statut = new Paragraph(occupe ? "🔴 Match en cours" : "🟢 Disponible");
        statut.getStyle()
            .set("margin", "0")
            .set("font-weight", "bold")
            .set("color", occupe ? "#d84315" : "#2e7d32");

        carte.add(titre, statut);

        if (occupe) {
            Paragraph infoMatch = new Paragraph("Ronde " + matchEnCours.getRondeNumero() + 
                                               " | Cliquez pour détails");
            infoMatch.getStyle()
                .set("margin", "8px 0 0 0")
                .set("font-size", "12px")
                .set("color", "#666")
                .set("font-style", "italic");
            carte.add(infoMatch);
        }

        return carte;
    }

    private MatchInfo getMatchEnCours(Connection con, int terrainId) throws SQLException {
        String sql = "SELECT m.id, m.score_e1, m.score_e2, m.statut, " +
                    "r.numero as ronde_num, t.nom as terrain_nom " +
                    "FROM matchs m " +
                    "INNER JOIN ronde r ON m.ronde_id = r.id " +
                    "INNER JOIN terrain t ON m.terrain_id = t.id " +
                    "WHERE m.terrain_id = ? AND m.statut != 'CLOS' " +
                    "ORDER BY r.debut DESC LIMIT 1";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, terrainId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new MatchInfo(
                        rs.getInt("id"),
                        rs.getInt("ronde_num"),
                        rs.getString("terrain_nom"),
                        rs.getInt("score_e1"),
                        rs.getInt("score_e2")
                    );
                }
            }
        }
        return null;
    }

    private void afficherMatchDialog(MatchInfo matchInfo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("🏆 Match en cours - Ronde " + matchInfo.getRondeNumero());
        dialog.setWidth("700px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Info match
        Div matchBox = new Div();
        matchBox.getStyle()
            .set("background-color", "#fff3cd")
            .set("padding", "15px")
            .set("border-radius", "8px")
            .set("margin-bottom", "15px");
        
        H4 matchTitle = new H4("⚽ Match #" + matchInfo.getMatchId());
        matchTitle.getStyle().set("margin", "0 0 10px 0");
        
        Paragraph score = new Paragraph("Score actuel : " + 
                                       matchInfo.getScoreEquipe1() + " - " + 
                                       matchInfo.getScoreEquipe2());
        score.getStyle()
            .set("margin", "0")
            .set("font-size", "18px")
            .set("font-weight", "bold");
        
        matchBox.add(matchTitle, score);
        layout.add(matchBox);

        try (Connection con = ConnectionPool.getConnection()) {
            // Récupérer les équipes
            List<EquipeInfo> equipes = getEquipesDuMatch(con, matchInfo.getMatchId());
            
            if (equipes.isEmpty()) {
                layout.add(new Paragraph("⚠️ Aucune équipe trouvée pour ce match"));
            } else {
                H4 equipesTitle = new H4("👥 Équipes participantes");
                layout.add(equipesTitle);
                
                for (int i = 0; i < equipes.size(); i++) {
                    EquipeInfo equipe = equipes.get(i);
                    int equipeNum = i + 1;
                    
                    Div equipeCard = creerCarteEquipe(con, equipe, equipeNum, 
                                                     equipeNum == 1 ? matchInfo.getScoreEquipe1() : 
                                                     matchInfo.getScoreEquipe2());
                    layout.add(equipeCard);
                }
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            layout.add(new Paragraph("❌ Erreur : " + ex.getMessage()));
        }

        Button fermerButton = new Button("Fermer", e -> dialog.close());
        fermerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(fermerButton);

        dialog.add(layout);
        dialog.open();
    }

    private Div creerCarteEquipe(Connection con, EquipeInfo equipe, int equipeNum, int score) 
            throws SQLException {
        Div card = new Div();
        card.getStyle()
            .set("background-color", equipeNum == 1 ? "#e3f2fd" : "#f3e5f5")
            .set("border", "2px solid " + (equipeNum == 1 ? "#2196f3" : "#9c27b0"))
            .set("border-radius", "8px")
            .set("padding", "15px")
            .set("margin-bottom", "10px")
            .set("cursor", "pointer")
            .set("transition", "transform 0.2s");

        card.addClickListener(e -> afficherJoueursDialog(equipe.getEquipeId(), equipeNum));

        card.getElement().addEventListener("mouseenter", ev -> {
            card.getStyle().set("transform", "scale(1.02)");
        });
        
        card.getElement().addEventListener("mouseleave", ev -> {
            card.getStyle().set("transform", "scale(1)");
        });

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        H5 titre = new H5("👥 Équipe " + equipeNum + " - ID: " + equipe.getEquipeId());
        titre.getStyle().set("margin", "0");
        
        Span scoreSpan = new Span("Score: " + score);
        scoreSpan.getStyle()
            .set("font-size", "18px")
            .set("font-weight", "bold")
            .set("color", "#333");
        
        header.add(titre, scoreSpan);
        card.add(header);

        // Afficher les joueurs
        List<JoueurInfo> joueurs = getJoueursEquipe(con, equipe.getEquipeId());
        if (!joueurs.isEmpty()) {
            Paragraph joueursText = new Paragraph("Joueurs : " + 
                joueurs.stream()
                    .map(JoueurInfo::getNom)
                    .collect(Collectors.joining(", ")));
            joueursText.getStyle()
                .set("margin", "8px 0 0 0")
                .set("font-size", "14px")
                .set("color", "#555");
            card.add(joueursText);
        }

        Paragraph cliquez = new Paragraph("👆 Cliquez pour voir les détails des joueurs");
        cliquez.getStyle()
            .set("margin", "8px 0 0 0")
            .set("font-size", "12px")
            .set("font-style", "italic")
            .set("color", "#999");
        card.add(cliquez);

        return card;
    }

    private void afficherJoueursDialog(int equipeId, int equipeNum) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("👥 Joueurs de l'équipe " + equipeNum);
        dialog.setWidth("800px");

        VerticalLayout layout = new VerticalLayout();

        try (Connection con = ConnectionPool.getConnection()) {
            List<JoueurInfo> joueurs = getJoueursEquipe(con, equipeId);
            
            if (joueurs.isEmpty()) {
                layout.add(new Paragraph("⚠️ Aucun joueur dans cette équipe"));
            } else {
                Grid<JoueurInfo> grid = new Grid<>(JoueurInfo.class, false);
                
                grid.addColumn(JoueurInfo::getId)
                    .setHeader("ID")
                    .setAutoWidth(true);
                
                grid.addColumn(JoueurInfo::getNom)
                    .setHeader("Nom")
                    .setAutoWidth(true);
                
                grid.addColumn(JoueurInfo::getPrenom)
                    .setHeader("Prénom")
                    .setAutoWidth(true);
                
                grid.addColumn(j -> j.getSexe().equals("M") ? "👨 Homme" : "👩 Femme")
                    .setHeader("Sexe")
                    .setAutoWidth(true);
                
                grid.addColumn(JoueurInfo::getNiveau)
                    .setHeader("Niveau")
                    .setAutoWidth(true);
                
                grid.addColumn(j -> j.getTaille() + " cm")
                    .setHeader("Taille")
                    .setAutoWidth(true);
                
                grid.setItems(joueurs);
                grid.setHeight("400px");
                
                layout.add(grid);
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            layout.add(new Paragraph("❌ Erreur : " + ex.getMessage()));
        }

        Button fermerButton = new Button("Fermer", e -> dialog.close());
        fermerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(fermerButton);

        dialog.add(layout);
        dialog.open();
    }

    private List<EquipeInfo> getEquipesDuMatch(Connection con, int matchId) throws SQLException {
        List<EquipeInfo> equipes = new ArrayList<>();
        
        String sql = "SELECT DISTINCT e.id, e.nom " +
                    "FROM equipe e " +
                    "INNER JOIN matchs m ON (m.equipe1_id = e.id OR m.equipe2_id = e.id) " +
                    "WHERE m.id = ?";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipes.add(new EquipeInfo(
                        rs.getInt("id"),
                        rs.getString("nom")
                    ));
                }
            }
        }
        
        return equipes;
    }

    private List<JoueurInfo> getJoueursEquipe(Connection con, int equipeId) throws SQLException {
        List<JoueurInfo> joueurs = new ArrayList<>();
        
        String sql = "SELECT j.id, j.nom, j.prenom, j.sexe, j.niveau, j.taille " +
                    "FROM joueur j " +
                    "INNER JOIN contient c ON c.joueur_id = j.id " +
                    "WHERE c.equipe_id = ?";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    joueurs.add(new JoueurInfo(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("sexe"),
                        rs.getInt("niveau"),
                        rs.getInt("taille")
                    ));
                }
            }
        }
        
        return joueurs;
    }

    // Classes internes pour stocker les informations
    private static class MatchInfo {
        private final int matchId;
        private final int rondeNumero;
        private final String terrainNom;
        private final int scoreEquipe1;
        private final int scoreEquipe2;

        public MatchInfo(int matchId, int rondeNumero, String terrainNom, 
                        int scoreEquipe1, int scoreEquipe2) {
            this.matchId = matchId;
            this.rondeNumero = rondeNumero;
            this.terrainNom = terrainNom;
            this.scoreEquipe1 = scoreEquipe1;
            this.scoreEquipe2 = scoreEquipe2;
        }

        public int getMatchId() { return matchId; }
        public int getRondeNumero() { return rondeNumero; }
        public String getTerrainNom() { return terrainNom; }
        public int getScoreEquipe1() { return scoreEquipe1; }
        public int getScoreEquipe2() { return scoreEquipe2; }
    }

    private static class EquipeInfo {
        private final int equipeId;
        private final String nom;

        public EquipeInfo(int equipeId, String nom) {
            this.equipeId = equipeId;
            this.nom = nom;
        }

        public int getEquipeId() { return equipeId; }
        public String getNom() { return nom; }
    }

    private static class JoueurInfo {
        private final int id;
        private final String nom;
        private final String prenom;
        private final String sexe;
        private final int niveau;
        private final int taille;

        public JoueurInfo(int id, String nom, String prenom, String sexe, 
                         int niveau, int taille) {
            this.id = id;
            this.nom = nom;
            this.prenom = prenom;
            this.sexe = sexe;
            this.niveau = niveau;
            this.taille = taille;
        }

        public int getId() { return id; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public String getSexe() { return sexe; }
        public int getNiveau() { return niveau; }
        public int getTaille() { return taille; }
    }
}