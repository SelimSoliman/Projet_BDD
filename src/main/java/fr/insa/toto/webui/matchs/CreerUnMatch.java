package fr.insa.toto.webui.matchs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.TournoiMulti;
import fr.insa.toto.model.Ronde;
import fr.insa.toto.model.Equipe;
import fr.insa.toto.model.Match;
import fr.insa.toto.model.Terrain;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.extensions.ListeTournoisView;
import fr.insa.toto.webui.session.SessionInfo;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Vue pour créer un match sur le tournoi actif.
 */
@Route(value = "matchs/creer", layout = MainLayout.class)
@PageTitle("Créer un match")
public class CreerUnMatch extends VerticalLayout {

    private Paragraph tournoiInfo;
    private Select<Ronde> rondeSelect;
    private Select<Equipe> equipe1Select;
    private Select<Equipe> equipe2Select;
    private Button creerMatchButton;

    public CreerUnMatch() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add(new Paragraph("Seuls les administrateurs peuvent créer des matchs."));
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        add(new H2("Créer un nouveau match"));

        // Vérifier qu'un tournoi est actif
        TournoiMulti tournoiActif = SessionInfo.getTournoiActif();

        if (tournoiActif == null) {
            // Aucun tournoi actif
            Paragraph warning = new Paragraph("⚠️ Aucun tournoi actif sélectionné.");
            warning.getStyle()
                .set("background-color", "#fff3e0")
                .set("padding", "10px")
                .set("border-radius", "5px")
                .set("border-left", "4px solid #ff9800")
                .set("font-weight", "bold");
            add(warning);

            add(new Paragraph("Vous devez d'abord sélectionner un tournoi dans la liste " +
                             "avant de pouvoir créer un match."));
            
            Button goToListeButton = new Button("📋 Aller à la liste des tournois", e -> {
                getUI().ifPresent(ui -> ui.navigate(ListeTournoisView.class));
            });
            goToListeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(goToListeButton);
            return;
        }

        // Afficher les infos du tournoi actif
        tournoiInfo = new Paragraph();
        tournoiInfo.setText("🎯 Tournoi actif : " + tournoiActif.getNom() + 
                           " (ID: " + tournoiActif.getId() + ")");
        tournoiInfo.getStyle()
            .set("background-color", "#e7f5ff")
            .set("padding", "15px")
            .set("border-radius", "5px")
            .set("border-left", "4px solid #1976d2")
            .set("font-weight", "bold")
            .set("margin-bottom", "20px");
        add(tournoiInfo);

        // Sélection de la ronde
        rondeSelect = new Select<>();
        rondeSelect.setLabel("Sélectionner une ronde");
        rondeSelect.setPlaceholder("Choisir une ronde...");
        rondeSelect.setWidthFull();
        rondeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                chargerEquipes(e.getValue());
            }
        });
        add(rondeSelect);

        // Sélection équipe 1
        equipe1Select = new Select<>();
        equipe1Select.setLabel("Équipe 1");
        equipe1Select.setPlaceholder("Choisir l'équipe 1...");
        equipe1Select.setWidthFull();
        equipe1Select.setEnabled(false);
        add(equipe1Select);

        // Sélection équipe 2
        equipe2Select = new Select<>();
        equipe2Select.setLabel("Équipe 2");
        equipe2Select.setPlaceholder("Choisir l'équipe 2...");
        equipe2Select.setWidthFull();
        equipe2Select.setEnabled(false);
        add(equipe2Select);

        // Bouton créer match
        creerMatchButton = new Button("✨ Créer le match", e -> creerMatch());
        creerMatchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        creerMatchButton.setEnabled(false);
        add(creerMatchButton);

        // Activer le bouton quand les deux équipes sont sélectionnées
        equipe1Select.addValueChangeListener(e -> updateButtonState());
        equipe2Select.addValueChangeListener(e -> updateButtonState());

        // Charger les rondes du tournoi actif
        chargerRondes(tournoiActif);
    }

    private void chargerRondes(TournoiMulti tournoi) {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Ronde> rondes = Ronde.toutesLesRondesDuTournoi(con, tournoi.getId());
            
            if (rondes.isEmpty()) {
                Notification.show("⚠️ Aucune ronde trouvée pour ce tournoi. Créez d'abord une ronde.", 
                                5000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            rondeSelect.setItems(rondes);
            rondeSelect.setItemLabelGenerator(r -> "Ronde #" + r.getId());

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur lors du chargement des rondes : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void chargerEquipes(Ronde ronde) {
        try (Connection con = ConnectionPool.getConnection()) {
            List<Equipe> equipes = Equipe.toutesLesEquipesDeLaRonde(con, ronde.getId());
            
            if (equipes.isEmpty()) {
                Notification.show("⚠️ Aucune équipe trouvée pour cette ronde.", 
                                3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_WARNING);
                equipe1Select.setEnabled(false);
                equipe2Select.setEnabled(false);
                return;
            }

            equipe1Select.setItems(equipes);
            equipe1Select.setItemLabelGenerator(e -> "Équipe #" + e.getId());
            equipe1Select.setEnabled(true);

            equipe2Select.setItems(equipes);
            equipe2Select.setItemLabelGenerator(e -> "Équipe #" + e.getId());
            equipe2Select.setEnabled(true);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("❌ Erreur lors du chargement des équipes : " + ex.getMessage(), 
                            5000, Notification.Position.MIDDLE)
                       .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateButtonState() {
        boolean enabled = equipe1Select.getValue() != null && 
                         equipe2Select.getValue() != null &&
                         !equipe1Select.getValue().equals(equipe2Select.getValue());
        creerMatchButton.setEnabled(enabled);
    }

private void creerMatch() {
    Ronde ronde = rondeSelect.getValue();
    Equipe equipe1 = equipe1Select.getValue();
    Equipe equipe2 = equipe2Select.getValue();

    if (ronde == null || equipe1 == null || equipe2 == null) {
        Notification.show("⚠️ Veuillez sélectionner une ronde et deux équipes",
                3000, Notification.Position.MIDDLE);
        return;
    }
    if (equipe1.getId() == equipe2.getId()) {
        Notification.show("⚠️ Les deux équipes doivent être différentes",
                3000, Notification.Position.MIDDLE);
        return;
    }

    try (Connection con = ConnectionPool.getConnection()) {

        // ✅ terrain : tu peux mettre null si tu ne gères pas encore la sélection de terrain
        Terrain terrain = null;

        // ✅ bon constructeur
        Match match = new Match(ronde, terrain);

        // ✅ optionnel : mettre des scores initiaux si tu veux (sinon 0 par défaut)
        match.setScoreEquipe1(0);
        match.setScoreEquipe2(0);

        // ✅ insert dans "matchs"
        match.saveInDB(con);

        // ⚠️ IMPORTANT :
        // ton Match crée en mémoire 2 Equipe(this,1) et Equipe(this,2)
        // MAIS ton écran te fait sélectionner des Equipes venant de la BD.
        // Donc ton système "sélectionner equipe1/equipe2" n’est PAS compatible
        // avec le modèle Match actuel (qui crée ses propres équipes).
        //
        // => pour l’instant, NE sélectionne pas equipe1/equipe2 : supprime ces selects
        // => ou alors change le modèle (plus lourd).
        //
        // Je te conseille : supprimer equipe1Select/equipe2Select et créer les équipes automatiquement.

        Notification.show("✅ Match créé (ID: " + match.getId() + ")",
                4000, Notification.Position.MIDDLE);

    } catch (SQLException ex) {
        ex.printStackTrace();
        Notification.show("❌ Erreur : " + ex.getMessage(),
                5000, Notification.Position.MIDDLE);
    } } }
