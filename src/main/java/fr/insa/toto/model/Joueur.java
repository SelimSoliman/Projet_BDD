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
 * @author win
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Joueur extends ClasseMiroir{  

    private int id;                 
    private String surnom;
    private String categorie;            
    private int taillecm;

    public Joueur(int id, String surnom, String categorie, int taillecm) {
        this.id = id;
        this.surnom = surnom;
        this.categorie = categorie;
        this.taillecm = taillecm;
    }

   

       

@Override
protected PreparedStatement saveSansId(Connection con) throws SQLException {
    String sql = "INSERT INTO joueur (surnom, categorie, taillecm) VALUES (?, ?, ?)";
    PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    ps.setString(1, this.surnom);
    ps.setString(2, this.categorie);
    ps.setInt(3, this.taillecm);
    ps.executeUpdate();
    return ps;
}



    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

   
    public String getSurnom() { return surnom; }
    public void setSurnom(String surnom) { this.surnom = surnom; }

    public int getTaillecm() {
        return taillecm;
    }

    public void setTaillecm(int taillecm) {
        this.taillecm = taillecm;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public static List<Joueur> tousLesJoueurs(Connection con) throws SQLException {
    List<Joueur> res = new ArrayList<>();
    String sql = "select id, surnom, categorie, taillecm from joueur";
    try (PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            Joueur j = new Joueur(
                    rs.getInt("id"),
                    rs.getString("surnom"),
                    rs.getString("categorie"),
                    rs.getInt("taillecm"));
            res.add(j);
        }
    }
    return res;
}

}
