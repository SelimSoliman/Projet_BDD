/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is ecole of CoursBeuvron.

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
import java.sql.Statement;
import java.util.List;

/**
 *
 * @author francois
 */
public class GestionBDD {

    /**
     *
     * @param con
     * @throws SQLException
     */
public static void creeSchema(Connection con) throws SQLException {
    try {
        con.setAutoCommit(false);
        try (Statement st = con.createStatement()) {

            // ----- tables "exemple" utilisateur/loisir -----
            st.executeUpdate("create table utilisateur ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " surnom varchar(30) not null unique,"
                    + " pass varchar(20) not null,"
                    + " role integer not null"
                    + ")");

            st.executeUpdate("create table loisir ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(20) not null unique,"
                    + " description text not null"
                    + ")");

            st.executeUpdate("create table pratique ( "
                    + " idutilisateur integer not null,"
                    + " idloisir integer not null,"
                    + " niveau integer not null"
                    + ")");

            st.executeUpdate("create table apprecie ( "
                    + " u1 integer not null,"
                    + " u2 integer not null"
                    + ")");

            st.executeUpdate("alter table apprecie "
                    + " add constraint fk_apprecie_u1 "
                    + " foreign key (u1) references utilisateur(id)");

            st.executeUpdate("alter table apprecie "
                    + " add constraint fk_apprecie_u2 "
                    + " foreign key (u2) references utilisateur(id)");

            st.executeUpdate("alter table pratique "
                    + " add constraint fk_pratique_idutilisateur "
                    + " foreign key (idutilisateur) references utilisateur(id)");

            st.executeUpdate("alter table pratique "
                    + " add constraint fk_pratique_idloisir "
                    + " foreign key (idloisir) references loisir(id)");

            // ----- tables du tournoi -----

            // table Joueur (nécessaire pour les FKs plus bas)
            st.executeUpdate("create table Joueur ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " surnom varchar(30) not null unique,"
                    + " categorie char(1),"
                    + " taillecm integer"
                    + ")");

            // Terrain
            st.executeUpdate("create table Terrain ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " nom varchar(100) not null unique,"
                    + " disponible boolean not null"
                    + ")");

            // Ronde
            st.executeUpdate("create table Ronde ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " numero integer not null,"
                    + " debut timestamp not null,"
                    + " close boolean not null"
                    + ")");

            // Matchs
            st.executeUpdate("create table Matchs ( "
                    + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                    + " ronde_id integer not null,"
                    + " score_e1 integer not null,"
                    + " score_e2 integer not null,"
                    + " statut varchar(20) not null"
                    + ")");

            // table d'association Match / Joueur
            st.executeUpdate("create table Match_Joueur ( "
                    + " match_id integer not null,"
                    + " joueur_id integer not null"
                    + ")");

            // clés étrangères
            st.executeUpdate("alter table Matchs "
                    + " add constraint fk_match_ronde "
                    + " foreign key (ronde_id) references Ronde(id)");

            st.executeUpdate("alter table Match_Joueur "
                    + " add constraint fk_mj_match "
                    + " foreign key (match_id) references Matchs(id)");

            st.executeUpdate("alter table Match_Joueur "
                    + " add constraint fk_mj_joueur "
                    + " foreign key (joueur_id) references Joueur(id)");

            // tout s'est bien passé
            con.commit();
        }
    } catch (SQLException ex) {
        con.rollback();
        throw ex;
    } finally {
        con.setAutoCommit(true);
    }
}


    /**
     *
     * @param con
     * @throws SQLException
     */
    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            try {
                st.executeUpdate(
                        "alter table utilisateur "
                        + "drop constraint fk_utilisateur_u1");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate(
                        "alter table utilisateur "
                        + "drop constraint fk_utilisateur_u2");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate(
                        "alter table pratique "
                        + "drop constraint fk_pratique_idutilisateur");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate(
                        "alter table pratique "
                        + "drop constraint fk_pratique_idloisir");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table apprecie");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table pratique");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table loisir");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table utilisateur");
            } catch (SQLException ex) {
            }
            try {
            st.executeUpdate(
                    "alter table Matchs "
                    + "drop constraint fk_match_ronde");
        } catch (SQLException ex) {
            // on ignore si la contrainte n'existe pas
        }

        // Ensuite supprimer les tables dans l'ordre dépendance -> parent
        try {
            st.executeUpdate("drop table Matchs");
        } catch (SQLException ex) {
        }

        try {
            st.executeUpdate("drop table Ronde");
        } catch (SQLException ex) {
        }

        try {
            st.executeUpdate("drop table Terrain");
        } catch (SQLException ex) {
        }
    
        }
    }

    /**
     *
     * @param con
     * @throws SQLException
     */
    public static void razBdd(Connection con) throws SQLException {
    try (Statement st = con.createStatement()) {

        // tables avec dépendances d'abord
        st.executeUpdate("DROP TABLE IF EXISTS Match_Joueur");
        st.executeUpdate("DROP TABLE IF EXISTS Matchs");
        st.executeUpdate("DROP TABLE IF EXISTS Ronde");
        st.executeUpdate("DROP TABLE IF EXISTS Terrain");

        // tables simples
        st.executeUpdate("DROP TABLE IF EXISTS Joueur");
        st.executeUpdate("DROP TABLE IF EXISTS pratique");
        st.executeUpdate("DROP TABLE IF EXISTS apprecie");
        st.executeUpdate("DROP TABLE IF EXISTS loisir");
        st.executeUpdate("DROP TABLE IF EXISTS utilisateur");
    }

    // recréation propre
    creeSchema(con);
}


    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            razBdd(con);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
    // for (var u : User) {
    // u.saveInDB(con);
    }

