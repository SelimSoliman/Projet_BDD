package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.*;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Route(value = "rondes-matchs/gestion", layout = MainLayout.class)
@PageTitle("Gestion des rondes et matchs")
public class GestionRondesMatchsView extends VerticalLayout {

    private Grid<MatchInfo> gridMatchs;
    private Button creerRondeButton;
    private Button rafraichirButton;
    private ComboBox<TournoiMulti> tournoiCombo;
    private Paragraph stats;

    // ✅ panneau détails
    private Div detailsBox;

    public GestionRondesMatchsView() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add(new Paragraph("Seuls les administrateurs peuvent gérer les rondes et matchs."));
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1400px");

        add(new H2("⚽ Gestion des rondes et matchs"));

        // Info rapide
        Paragraph info = new Paragraph();
        info.setText("💡 Sélectionnez un tournoi puis créez des rondes avec des matchs directement depuis cette interface.");
        info.getStyle()
                .set("background-color", "#e3f2fd")
                .set("padding", "15px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #2196f3");
        add(info);

        // Sélection du tournoi
        tournoiCombo = new ComboBox<>("Tournoi");
        tournoiCombo.setItemLabelGenerator(TournoiMulti::getNom);
        tournoiCombo.setWidthFull();
        tournoiCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                creerRondeButton.setEnabled(true);
                try {
                    chargerMatchs(e.getValue());
                    resetDetails();
                } catch (SQLException ex) {
                    Notification.show("❌ Erreur : " + ex.getMessage(),
                                    5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                creerRondeButton.setEnabled(false);
                resetDetails();
            }
        });
        add(tournoiCombo);

        // Boutons d'action
        HorizontalLayout actionsLayout = new HorizontalLayout();

        creerRondeButton = new Button("➕ Créer une nouvelle ronde", e -> ouvrirDialogCreerRonde());
        creerRondeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        creerRondeButton.setEnabled(false);

        rafraichirButton = new Button("🔄 Rafraîchir", e -> {
            if (tournoiCombo.getValue() != null) {
                try {
                    chargerMatchs(tournoiCombo.getValue());
                    resetDetails();
                } catch (SQLException ex) {
                    Notification.show("❌ Erreur : " + ex.getMessage(),
                                    5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        rafraichirButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        actionsLayout.add(creerRondeButton, rafraichirButton);
        add(actionsLayout);

        // Section MATCHS
        add(new H3("🎾 Matchs du tournoi"));

        // ✅ panneau détails (au-dessus du tableau)
        detailsBox = new Div();
        detailsBox.getStyle()
                .set("background-color", "#f8f9fa")
                .set("border", "1px solid #dee2e6")
                .set("border-radius", "8px")
                .set("padding", "12px")
                .set("margin-bottom", "12px");
        detailsBox.setText("➡️ Sélectionne un match pour voir ses détails (ronde, terrain, équipes…).");
        add(detailsBox);

        // ✅ UNE SEULE grid
        gridMatchs = new Grid<>(MatchInfo.class, false);
        gridMatchs.setSelectionMode(Grid.SelectionMode.SINGLE);

        gridMatchs.addColumn(MatchInfo::getIdMatch).setHeader("ID Match").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getRonde).setHeader("Ronde").setAutoWidth(true);
        gridMatchs.addColumn(MatchInfo::getInfo).setHeader("Informations").setAutoWidth(true).setFlexGrow(1);

        gridMatchs.setWidthFull();
        add(gridMatchs);

        // ✅ sélection => affiche détails
        gridMatchs.asSingleSelect().addValueChangeListener(e -> {
            MatchInfo sel = e.getValue();
            if (sel == null) {
                resetDetails();
                return;
            }
            try (Connection con = ConnectionPool.getConnection()) {
                detailsBox.removeAll();
                detailsBox.add(creerDetailsMatch(con, sel.getIdMatch()));
            } catch (SQLException ex) {
                detailsBox.removeAll();
                detailsBox.add(new Paragraph("❌ Erreur détails : " + ex.getMessage()));
            }
        });

        // Statistiques
        stats = new Paragraph();
        stats.getStyle()
                .set("padding", "15px")
                .set("border-radius", "5px")
                .set("margin-top", "20px");
        add(stats);

        // Chargement initial
        try {
            chargerTournois();
        } catch (SQLException ex) {
            Notification.show("❌ Erreur de chargement : " + ex.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void resetDetails() {
        detailsBox.removeAll();
        detailsBox.setText("➡️ Sélectionne un match pour voir ses détails (ronde, terrain, équipes…).");
    }

    private void chargerTournois() throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            List<TournoiMulti> tournois = TournoiMulti.tousLesTournois(con);
            tournoiCombo.setItems(tournois);

            if (!tournois.isEmpty()) {
                tournoiCombo.setValue(tournois.get(0));
            }
        }
    }

    private void chargerMatchs(TournoiMulti tournoi) throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Match> matchs = new ArrayList<>();

            String sql = "SELECT m.id, m.ronde_id, m.score_e1, m.score_e2, m.statut, r.numero as ronde_numero " +
                    "FROM matchs m " +
                    "INNER JOIN ronde r ON m.ronde_id = r.id " +
                    "WHERE r.id_tournoi = ? " +
                    "ORDER BY r.numero, m.id";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, tournoi.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int matchId = rs.getInt("id");
                        int scoreE1 = rs.getInt("score_e1");
                        int scoreE2 = rs.getInt("score_e2");
                        String statut = rs.getString("statut");

                        Match m = new Match(matchId, null, null, scoreE1, scoreE2,
                                Match.Statut.valueOf(statut));
                        matchs.add(m);
                    }
                }
            }

            List<MatchInfo> matchsInfo = new ArrayList<>();

            for (Match m : matchs) {
                StringBuilder info = new StringBuilder();
                info.append("Scores: ").append(m.getScoreEquipe1());
                info.append(" - ").append(m.getScoreEquipe2());
                info.append(" | ").append(m.estClos() ? "✅ Terminé" : "⏳ En cours");

                int rondeNum = getRondeNumero(con, m.getId());
                matchsInfo.add(new MatchInfo(m.getId(), rondeNum, info.toString()));
            }

            gridMatchs.setItems(matchsInfo);

            if (matchsInfo.isEmpty()) {
                stats.setText("⚠️ Aucun match trouvé. Créez une ronde pour générer des matchs.");
                stats.getStyle().set("background-color", "#fff3cd");
            } else {
                long termines = matchsInfo.stream().filter(m -> m.getInfo().contains("Terminé")).count();
                stats.setText("📊 Total : " + matchsInfo.size() + " match(s) | " +
                        "✅ Terminés : " + termines + " | " +
                        "⏳ En cours : " + (matchsInfo.size() - termines));
                stats.getStyle().set("background-color", "#e8f5e9");
            }
        }
    }

    private int getRondeNumero(Connection con, int matchId) throws SQLException {
        String sql = "SELECT r.numero FROM ronde r " +
                "INNER JOIN matchs m ON m.ronde_id = r.id " +
                "WHERE m.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("numero");
            }
        }
        return 0;
    }

    // ====== ✅ Joueurs cliquables dans le détail match ======

    private static class JoueurRef {
        int id;
        String prenom;
        String nom;
        String surnom;

        JoueurRef(int id, String prenom, String nom, String surnom) {
            this.id = id;
            this.prenom = prenom;
            this.nom = nom;
            this.surnom = surnom;
        }

        String label() {
            String p = (prenom == null ? "" : prenom);
            String n = (nom == null ? "" : nom);
            return (p + " " + n).trim();
        }
    }

    private List<JoueurRef> getJoueursEquipe(Connection con, int matchId, int numeroEquipe) throws SQLException {
        String sql = """
                SELECT j.id, j.nom, j.prenom, j.surnom
                FROM match_joueur mj
                JOIN joueur j ON j.id = mj.id_joueur
                WHERE mj.id_match = ? AND mj.numero_equipe = ?
                ORDER BY j.nom, j.prenom
                """;
        List<JoueurRef> res = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, numeroEquipe);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new JoueurRef(
                            rs.getInt("id"),
                            rs.getString("prenom"),
                            rs.getString("nom"),
                            rs.getString("surnom")
                    ));
                }
            }
        }
        return res;
    }

   private void ouvrirDialogJoueur(int joueurId) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("👤 Détails joueur");
    dialog.setWidth("520px");

    VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(false);
    layout.setPadding(false);

    try (Connection con = ConnectionPool.getConnection()) {

        // ✅ colonne correcte : date_naissance
        String sql = "SELECT id, surnom, nom, prenom, sexe, categorie, taillecm, date_naissance " +
                     "FROM joueur WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, joueurId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    layout.add(new Paragraph("Joueur introuvable (id=" + joueurId + ")"));
                } else {
                    String surnom = rs.getString("surnom");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    String sexe = rs.getString("sexe");
                    String cat = rs.getString("categorie");
                    Integer taille = (Integer) rs.getObject("taillecm");

                    // ✅ champ correct : date_naissance
                    java.sql.Date dn = rs.getDate("date_naissance");

                    layout.add(new H3((surnom != null ? surnom : "Joueur") + " (id=" + joueurId + ")"));
                    layout.add(new Paragraph("Nom : " + safe(prenom) + " " + safe(nom)));
                    layout.add(new Paragraph("Sexe : " + safe(sexe) + " | Catégorie : " + safe(cat)));
                    layout.add(new Paragraph("Taille : " + (taille == null ? "-" : (taille + " cm"))));

                    if (dn != null) {
                        LocalDate birth = dn.toLocalDate();
                        int age = Period.between(birth, LocalDate.now()).getYears();
                        layout.add(new Paragraph("Naissance : " + birth + " (Âge : " + age + " ans)"));
                    } else {
                        layout.add(new Paragraph("Naissance : -"));
                    }
                }
            }
        }

    } catch (SQLException ex) {
        layout.add(new Paragraph("❌ Erreur : " + ex.getMessage()));
    }

    Button close = new Button("Fermer", e -> dialog.close());
    close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    layout.add(close);

    dialog.add(layout);
    dialog.open();
}


    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    // ✅ DÉTAILS MATCH : ronde/terrain/statut/score + joueurs équipe1/équipe2 cliquables
    private Component creerDetailsMatch(Connection con, int matchId) throws SQLException {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(false);
        box.setSpacing(false);

        String sql = """
                SELECT m.id, m.score_e1, m.score_e2, m.statut,
                       r.numero AS ronde_numero,
                       t.nom AS terrain_nom
                FROM matchs m
                JOIN ronde r ON r.id = m.ronde_id
                LEFT JOIN terrain t ON t.id = m.terrain_id
                WHERE m.id = ?
                """;

        int rondeNumero = 0;
        String statut = "";
        int s1 = 0, s2 = 0;
        String terrainNom = "(non défini)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    rondeNumero = rs.getInt("ronde_numero");
                    statut = rs.getString("statut");
                    s1 = rs.getInt("score_e1");
                    s2 = rs.getInt("score_e2");
                    String tn = rs.getString("terrain_nom");
                    if (tn != null) terrainNom = tn;
                }
            }
        }

        H4 title = new H4("Détails — Match " + matchId);
        Paragraph p1 = new Paragraph("Ronde : " + rondeNumero + " | Terrain : " + terrainNom);
        Paragraph p2 = new Paragraph("Score : " + s1 + " - " + s2 + " | Statut : " + statut);

        UnorderedList equipe1 = new UnorderedList();
        equipe1.add(new ListItem("Équipe 1 :"));
        for (JoueurRef jr : getJoueursEquipe(con, matchId, 1)) {
            Button b = new Button(jr.label());
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            b.getStyle().set("padding", "0").set("font-weight", "600");
            b.addClickListener(e -> ouvrirDialogJoueur(jr.id));
            equipe1.add(new ListItem(b));
        }

        UnorderedList equipe2 = new UnorderedList();
        equipe2.add(new ListItem("Équipe 2 :"));
        for (JoueurRef jr : getJoueursEquipe(con, matchId, 2)) {
            Button b = new Button(jr.label());
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            b.getStyle().set("padding", "0").set("font-weight", "600");
            b.addClickListener(e -> ouvrirDialogJoueur(jr.id));
            equipe2.add(new ListItem(b));
        }

        box.add(title, p1, p2, equipe1, equipe2);
        return box;
    }

    // ====== le reste de ton code (création ronde / équipe / etc.) ======

    private void ouvrirDialogCreerRonde() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("➕ Créer une nouvelle ronde");
        dialog.setWidth("600px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        TournoiMulti tournoi = tournoiCombo.getValue();

        Paragraph info = new Paragraph();
        info.setText("🎾 Tournoi : " + tournoi.getNom() + "\n" +
                "🏟️ Terrains : " + tournoi.getNbTerrains() + "\n" +
                "👥 Joueurs par équipe : " + tournoi.getNbJoueursParEquipe());
        info.getStyle()
                .set("background-color", "#e3f2fd")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("white-space", "pre-line");
        layout.add(info);

        FormLayout form = new FormLayout();

        IntegerField nbMatchsField = new IntegerField("Nombre de matchs à créer");
        nbMatchsField.setValue(tournoi.getNbTerrains());
        nbMatchsField.setMin(1);
        nbMatchsField.setMax(20);
        nbMatchsField.setStepButtonsVisible(true);
        nbMatchsField.setHelperText("Généralement = nombre de terrains");

        form.add(nbMatchsField);
        layout.add(form);

        Paragraph warning = new Paragraph();
        warning.setText("⚠️ Les équipes seront générées aléatoirement parmi les joueurs inscrits au tournoi.");
        warning.getStyle()
                .set("background-color", "#fff3cd")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("font-size", "14px");
        layout.add(warning);

        HorizontalLayout buttons = new HorizontalLayout();

        Button creerButton = new Button("✅ Créer la ronde", e -> {
            try (Connection con = ConnectionPool.getConnection()) {
                int nbMatchs = nbMatchsField.getValue();

                boolean success = creerNouvelleRonde(con, tournoi, nbMatchs);

                if (success) {
                    Notification.show("✅ Ronde créée avec " + nbMatchs + " match(s) !",
                                    3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    chargerMatchs(tournoi);
                    resetDetails();
                    dialog.close();
                } else {
                    Notification.show("❌ Erreur lors de la création de la ronde",
                                    5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

            } catch (SQLException ex) {
                Notification.show("❌ Erreur : " + ex.getMessage(),
                                5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });
        creerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button annulerButton = new Button("Annuler", e -> dialog.close());

        buttons.add(creerButton, annulerButton);
        layout.add(buttons);

        dialog.add(layout);
        dialog.open();
    }

    private boolean creerNouvelleRonde(Connection con, TournoiMulti tournoi, int nbMatchs) throws SQLException {
        int numeroRonde = 1;
        String sqlCount = "SELECT MAX(numero) as max_numero FROM ronde WHERE id_tournoi = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlCount)) {
            ps.setInt(1, tournoi.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("max_numero") > 0) {
                    numeroRonde = rs.getInt("max_numero") + 1;
                }
            }
        }

        int rondeId;
        String sqlRonde = "INSERT INTO ronde (id_tournoi, numero, debut, close) VALUES (?, ?, NOW(), 0)";
        try (PreparedStatement ps = con.prepareStatement(sqlRonde, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tournoi.getId());
            ps.setInt(2, numeroRonde);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    rondeId = rs.getInt(1);
                } else {
                    return false;
                }
            }
        }

        List<Terrain> terrains = Terrain.tousLesTerrains(con);
        if (terrains.isEmpty()) {
            throw new SQLException("Aucun terrain disponible. Créez d'abord des terrains.");
        }

        tournoi.inscrireTousLesJoueurs(con);

        List<Joueur> joueurs = tournoi.getJoueursInscrits(con);
        if (joueurs.size() < nbMatchs * tournoi.getNbJoueursParEquipe() * 2) {
            throw new SQLException("Pas assez de joueurs inscrits. Il faut au moins " +
                    (nbMatchs * tournoi.getNbJoueursParEquipe() * 2) + " joueurs.");
        }

        java.util.Collections.shuffle(joueurs);

        int joueursParEquipe = tournoi.getNbJoueursParEquipe();
        int joueurIndex = 0;

        for (int i = 0; i < nbMatchs && joueurIndex + (joueursParEquipe * 2) <= joueurs.size(); i++) {
            Terrain terrain = terrains.get(i % terrains.size());

            String sqlMatch = "INSERT INTO matchs (ronde_id, terrain_id, score_e1, score_e2, statut) VALUES (?, ?, 0, 0, 'EN_COURS')";
            int matchId;
            try (PreparedStatement ps = con.prepareStatement(sqlMatch, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, rondeId);
                ps.setInt(2, terrain.getId());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        matchId = rs.getInt(1);
                    } else {
                        continue;
                    }
                }
            }

            creerEquipe(con, matchId, 1);
            for (int j = 0; j < joueursParEquipe && joueurIndex < joueurs.size(); j++) {
                ajouterJoueurEquipe(con, matchId, joueurs.get(joueurIndex++).getId(), 1);
            }

            creerEquipe(con, matchId, 2);
            for (int j = 0; j < joueursParEquipe && joueurIndex < joueurs.size(); j++) {
                ajouterJoueurEquipe(con, matchId, joueurs.get(joueurIndex++).getId(), 2);
            }
        }

        return true;
    }

    private int creerEquipe(Connection con, int matchId, int numero) throws SQLException {
        String sql = "INSERT INTO equipe (id_match, numero, score) VALUES (?, ?, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, matchId);
            ps.setInt(2, numero);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Impossible de créer l'équipe");
    }

    private void ajouterJoueurEquipe(Connection con, int matchId, int joueurId, int numeroEquipe) throws SQLException {
        String sql = "INSERT INTO match_joueur (id_match, id_joueur, numero_equipe) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, joueurId);
            ps.setInt(3, numeroEquipe);
            ps.executeUpdate();
        }
    }

    public static class MatchInfo {
        private int idMatch;
        private int ronde;
        private String info;

        public MatchInfo(int idMatch, int ronde, String info) {
            this.idMatch = idMatch;
            this.ronde = ronde;
            this.info = info;
        }

        public int getIdMatch() { return idMatch; }
        public int getRonde() { return ronde; }
        public String getInfo() { return info; }
    }
}
