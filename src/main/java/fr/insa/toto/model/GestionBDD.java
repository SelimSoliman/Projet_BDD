package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class GestionBDD {

    // ================== CREATION DU SCHEMA ==================

    public static void creeSchema(Connection con) throws SQLException {
        try {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {

                // ----- table utilisateur (roles admin / user) -----
                st.executeUpdate("create table utilisateur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " surnom varchar(30) not null unique,"
                        + " pass varchar(20) not null,"
                        + " role integer not null"
                        + ")");

                // Tournoi
                st.executeUpdate("create table tournoi ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(100) not null,"
                        + " nb_terrains integer not null,"
                        + " nb_joueurs_par_equipe integer not null"
                        + ")");

                // Joueur
                st.executeUpdate("create table joueur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " surnom varchar(30) not null unique,"
                        + " nom varchar(50),"
                        + " taillecm int,"
                        + " prenom varchar (50),"
                        + " categorie varchar(50),"
                        + " sexe char(1),"
                        + " date_naissance date"
                        + ")");

                // Terrain
                st.executeUpdate("create table terrain ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(100) not null unique,"
                        + " disponible boolean not null"
                        + ")");

                // Ronde
                st.executeUpdate("create table ronde ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " id_tournoi integer not null,"
                        + " numero integer not null,"
                        + " debut timestamp not null,"
                        + " close boolean not null"
                        + ")");

                // Matchs
                st.executeUpdate("create table matchs ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " ronde_id integer not null,"
                        + " terrain_id integer,"
                        + " score_e1 integer not null,"
                        + " score_e2 integer not null,"
                        + " statut varchar(20) not null"
                        + ")");

                // Equipe
                st.executeUpdate("create table equipe ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " id_match integer not null,"
                        + " numero integer not null,"
                        + " score integer not null"
                        + ")");

                // Match_Joueur
                st.executeUpdate("create table match_joueur ( "
                        + " id_match integer not null,"
                        + " id_joueur integer not null,"
                        + " numero_equipe integer not null,"
                        + " primary key (id_match, id_joueur)"
                        + ")");

                // ----- cles etrangeres -----

                st.executeUpdate("alter table ronde "
                        + " add constraint fk_ronde_tournoi "
                        + " foreign key (id_tournoi) references tournoi(id)");

                st.executeUpdate("alter table matchs "
                        + " add constraint fk_match_ronde "
                        + " foreign key (ronde_id) references ronde(id)");

                st.executeUpdate("alter table matchs "
                        + " add constraint fk_match_terrain "
                        + " foreign key (terrain_id) references terrain(id)");

                st.executeUpdate("alter table equipe "
                        + " add constraint fk_equipe_match "
                        + " foreign key (id_match) references matchs(id)");

                st.executeUpdate("alter table match_joueur "
                        + " add constraint fk_mj_match "
                        + " foreign key (id_match) references matchs(id)");

                st.executeUpdate("alter table match_joueur "
                        + " add constraint fk_mj_joueur "
                        + " foreign key (id_joueur) references joueur(id)");

                con.commit();
            }
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    // ================== SUPPRESSION COMPLETE DU SCHEMA ==================

    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {

            try { st.executeUpdate(
                    "alter table match_joueur drop constraint fk_mj_match"); } catch (SQLException ex) {}
            try { st.executeUpdate(
                    "alter table match_joueur drop constraint fk_mj_joueur"); } catch (SQLException ex) {}
            try { st.executeUpdate(
                    "alter table equipe drop constraint fk_equipe_match"); } catch (SQLException ex) {}
            try { st.executeUpdate(
                    "alter table matchs drop constraint fk_match_ronde"); } catch (SQLException ex) {}
            try { st.executeUpdate(
                    "alter table matchs drop constraint fk_match_terrain"); } catch (SQLException ex) {}
            try { st.executeUpdate(
                    "alter table ronde drop constraint fk_ronde_tournoi"); } catch (SQLException ex) {}

            try { st.executeUpdate("drop table match_joueur"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table equipe"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table matchs"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table ronde"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table terrain"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table joueur"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table tournoi"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table utilisateur"); } catch (SQLException ex) {}
        }
    }

    // ================== RAZ BDD (DROP + CREATE) ==================

    public static void razBdd(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("drop table if exists match_joueur");
            st.executeUpdate("drop table if exists equipe");
            st.executeUpdate("drop table if exists matchs");
            st.executeUpdate("drop table if exists ronde");
            st.executeUpdate("drop table if exists terrain");
            st.executeUpdate("drop table if exists joueur");
            st.executeUpdate("drop table if exists tournoi");
            st.executeUpdate("drop table if exists utilisateur");
        }
        creeSchema(con);
    }

    // ================== MAIN DE TEST ==================

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            razBdd(con);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
}
