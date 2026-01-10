package fr.insa.toto.webui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

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
                "Suivez l’évolution des matchs en temps réel, découvrez les équipes, " +
                "consultez les classements et vivez chaque ronde du tournoi au plus près. " +
                "Les organisateurs disposent d’outils dédiés pour gérer les joueurs, " +
                "les terrains et les résultats en toute simplicité. " +
                "Une expérience claire, rapide et pensée pour le jeu."
        );

        Button cta = new Button("Voir le classement");
        cta.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // ✅ adapte si tu as une route "classement"
        // cta.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("classement")));

        hero.add(title, p, cta);

        // STATS
        Div stats = new Div();
        stats.addClassName("stats");
        stats.add(statCard("0", "Joueurs"));
        stats.add(statCard("0", "Rondes"));
        stats.add(statCard("0", "Matchs en cours"));

        container.add(hero, stats);
        add(container);
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
