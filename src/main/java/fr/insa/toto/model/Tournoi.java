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
package fr.insa.toto.model;

/**
 *
 * @author ThinkPad
 */

import java.util.ArrayList;
import java.util.List;
import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Tournoi extends ClasseMiroir{

    private String nom;
    private int nbTerrains;
    private int nbJoueursParEquipe;

    private List<Joueur> joueurs = new ArrayList<>();
    private List<Ronde> rondes = new ArrayList<>();

    public Tournoi(String nom, int nbTerrains, int nbJoueursParEquipe) {
        this.nom = nom;
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }
@Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = "INSERT INTO Tournoi (nom, nb_terrains, nb_joueurs_par_equipe) VALUES (?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, this.nom);
        ps.setInt(2, this.nbTerrains);
        ps.setInt(3, this.nbJoueursParEquipe);

        return ps;
    }
    public void ajouterJoueur(Joueur j) {
        joueurs.add(j);
    }

    public List<Joueur> getJoueurs() {
        return joueurs;
    }

    public Ronde nouvelleronde() {
        Ronde r = new Ronde(rondes.size() + 1);
        rondes.add(r);
        return r;
    }

    public List<Ronde> getRondes() {
        return rondes;
    }

    public String getNom() { return nom; }
}

