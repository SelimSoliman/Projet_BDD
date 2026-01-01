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

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BdDTest {

    public static void createBdDTestV1(Connection con) throws SQLException {
        List<Utilisateur> users = List.of(
                new Utilisateur("toto", "p1", 1),
                new Utilisateur("titi", "p2", 2),
                new Utilisateur("tutu", "p2", 2)
        );
        for (Utilisateur u : users) {
            u.saveInDB(con);
        }
    }

    /**
     * Version sans Loisir / pratique / apprecie :
     * uniquement des utilisateurs.
     */
    public static void createBdDTestV2(Connection con) throws SQLException {
        createBdDTestV1(con);
    }

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            GestionBDD.razBdd(con);
            createBdDTestV2(con);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
}

