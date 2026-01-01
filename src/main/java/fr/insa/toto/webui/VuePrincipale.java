/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.toto.webui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 *
 * @author francois
 */
@Route(value = "",layout=MainLayout.class)
@PageTitle("Teqball")
public class VuePrincipale extends VerticalLayout {

    public VuePrincipale() {
        this.add(new H2("Bienvenue au site officiel du tournoi de Teqball"));
        this.add(new Paragraph ("Bienvenue sur la plateforme officielle du tournoi de Teqball.\n" +
"Suivez l’évolution des matchs en temps réel, découvrez les équipes, consultez les classements et vivez chaque ronde du tournoi au plus près.\n" +
"Les organisateurs disposent d’outils dédiés pour gérer les joueurs, les terrains et les résultats en toute simplicité.\n" +
"Une expérience claire, rapide et pensée pour le jeu."));
        
        
      
    }

}
