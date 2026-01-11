package fr.insa.toto.webui.extensions;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionPool;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.webui.MainLayout;
import fr.insa.toto.webui.session.SessionInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Route(value = "admin/initialisation", layout = MainLayout.class)
@PageTitle("Initialisation des données")
public class InitialisationDonneesView extends VerticalLayout {

    public InitialisationDonneesView() {
        if (!SessionInfo.adminConnected()) {
            add(new H2("Accès refusé"));
            add(new Paragraph("Seuls les administrateurs peuvent accéder à cette page."));
            return;
        }

        setPadding(true);
        setSpacing(true);
        setMaxWidth("900px");

        add(new H2("🔧 Initialisation des données"));

        // Section Joueurs
        Div sectionJoueurs = new Div();
        sectionJoueurs.getStyle()
            .set("background-color", "#e3f2fd")
            .set("padding", "20px")
            .set("border-radius", "8px")
            .set("margin-bottom", "20px");
        
        sectionJoueurs.add(new H3("👥 Joueurs de démonstration"));
        
        Paragraph descJoueurs = new Paragraph();
        descJoueurs.setText("Créez automatiquement 10 joueurs variés (différents niveaux, sexes, tailles) pour tester l'application.");
        sectionJoueurs.add(descJoueurs);
        
        Button creer10JoueursButton = new Button("➕ Créer 10 joueurs", e -> {
            try {
                int nbCrees = creer10Joueurs();
                if (nbCrees > 0) {
                    Notification.show("✅ " + nbCrees + " joueur(s) créé(s) avec succès !", 
                                    3000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("ℹ️ Les joueurs existent déjà.", 
                                    3000, Notification.Position.MIDDLE)
                               .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
            } catch (SQLException ex) {
                Notification.show("❌ Erreur : " + ex.getMessage(), 
                                5000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        creer10JoueursButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        
        sectionJoueurs.add(creer10JoueursButton);
        add(sectionJoueurs);

        // Section Liste des joueurs créés
        Div listeSection = new Div();
        listeSection.getStyle()
            .set("background-color", "#f5f5f5")
            .set("padding", "20px")
            .set("border-radius", "8px");
        
        listeSection.add(new H3("📋 Joueurs pré-configurés"));
        
        UnorderedList listeJoueurs = new UnorderedList();
        String[] joueurs = {
            "Alice Martin (F, 170cm, Intermédiaire)",
            "Bob Dupont (M, 180cm, Avancé)",
            "Clara Bernard (F, 165cm, Débutant)",
            "David Petit (M, 185cm, Expert)",
            "Emma Durand (F, 168cm, Intermédiaire)",
            "Florian Moreau (M, 178cm, Avancé)",
            "Gabrielle Simon (F, 162cm, Débutant)",
            "Hugo Laurent (M, 182cm, Expert)",
            "Inès Lefebvre (F, 166cm, Intermédiaire)",
            "Jules Roux (M, 175cm, Avancé)"
        };
        
        for (String j : joueurs) {
            listeJoueurs.add(new ListItem(j));
        }
        
        listeSection.add(listeJoueurs);
        add(listeSection);

        // Info
        Paragraph info = new Paragraph();
        info.setText("💡 Ces joueurs sont créés automatiquement au démarrage de l'application si aucun joueur n'existe. " +
                    "Vous pouvez aussi les créer manuellement avec ce bouton.");
        info.getStyle()
            .set("margin-top", "20px")
            .set("color", "#666")
            .set("font-style", "italic");
        add(info);
    }

    private int creer10Joueurs() throws SQLException {
        try (Connection con = ConnectionPool.getConnection()) {
            
            List<Joueur> joueursExistants = Joueur.tousLesJoueurs(con);
            
            String[][] joueurs = {
                {"Alice", "Martin", "Alice", "Intermédiaire", "170", "F"},
                {"Bob", "Dupont", "Bob", "Avancé", "180", "M"},
                {"Clara", "Bernard", "Clara", "Débutant", "165", "F"},
                {"David", "Petit", "David", "Expert", "185", "M"},
                {"Emma", "Durand", "Emma", "Intermédiaire", "168", "F"},
                {"Florian", "Moreau", "Flo", "Avancé", "178", "M"},
                {"Gabrielle", "Simon", "Gaby", "Débutant", "162", "F"},
                {"Hugo", "Laurent", "Hugo", "Expert", "182", "M"},
                {"Inès", "Lefebvre", "Inès", "Intermédiaire", "166", "F"},
                {"Jules", "Roux", "Jules", "Avancé", "175", "M"}
            };
            
            int joueursAjoutes = 0;
            
            for (String[] j : joueurs) {
                String nom = j[0];
                String prenom = j[1];
                String surnom = j[2];
                String categorie = j[3];
                int taille = Integer.parseInt(j[4]);
                String sexe = j[5];
                
                boolean existe = joueursExistants.stream()
                    .anyMatch(joueur -> joueur.getSurnom().equalsIgnoreCase(surnom));
                
                if (!existe) {
                    Joueur joueur = new Joueur(
                        surnom,
                        categorie,
                        taille,
                        nom,
                        prenom,
                        sexe,
                        LocalDate.now()
                    );
                    
                    joueur.saveInDB(con);
                    joueursAjoutes++;
                }
            }
            
            return joueursAjoutes;
        }
    }
}