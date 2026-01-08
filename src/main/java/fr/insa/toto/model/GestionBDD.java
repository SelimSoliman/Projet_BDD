package fr.insa.toto.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestion complète du schéma de la base de données
 * Inclut les tables de base + toutes les extensions
 */
public class GestionBDD {

    // ================== CREATION DU SCHEMA COMPLET ==================

    public static void creeSchema(Connection con) throws SQLException {
        try {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {

                // ========== TABLES DE BASE ==========

                // ----- Table utilisateur (roles admin / user) -----
                st.executeUpdate("create table utilisateur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " surnom varchar(30) not null unique,"
                        + " pass varchar(20) not null,"
                        + " role integer not null"
                        + ")");

                // ----- Table joueur -----
                st.executeUpdate("create table joueur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " surnom varchar(30) not null unique,"
                        + " nom varchar(50),"
                        + " prenom varchar(50),"
                        + " categorie varchar(50),"
                        + " taillecm int,"
                        + " sexe char(1),"
                        + " date_naissance date"
                        + ")");

                // ----- Table tournoi (avec extensions multi-tournoi) -----
                st.executeUpdate("create table tournoi ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(100) not null,"
                        + " nb_terrains integer not null,"
                        + " nb_joueurs_par_equipe integer not null,"
                        // Extension 2 : Multi-tournoi
                        + " date_creation timestamp default current_timestamp,"
                        + " date_debut timestamp,"
                        + " date_fin timestamp,"
                        + " statut varchar(20) default 'EN_PREPARATION'"
                        + ")");

                // ----- Table terrain (avec extension plan) -----
                st.executeUpdate("create table terrain ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(100) not null unique,"
                        + " disponible boolean not null,"
                        // Extension 3 : Terrains avec plan
                        + " chemin_plan varchar(500),"
                        + " description text"
                        + ")");

                // ----- Table ronde -----
                st.executeUpdate("create table ronde ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " id_tournoi integer not null,"
                        + " numero integer not null,"
                        + " debut timestamp not null,"
                        + " close boolean not null"
                        + ")");

                // ========== EXTENSION 4-5 : TYPES DE JEU ==========

                // ----- Table type_jeu -----
                st.executeUpdate("create table type_jeu ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(100) not null unique,"
                        + " nb_equipes integer not null,"
                        + " nb_joueurs_min integer not null,"
                        + " nb_joueurs_max integer not null,"
                        + " check (nb_joueurs_min > 0),"
                        + " check (nb_joueurs_max >= nb_joueurs_min),"
                        + " check (nb_equipes >= 2)"
                        + ")");

                // ----- Association terrain <-> type_jeu -----
                st.executeUpdate("create table terrain_type_jeu ( "
                        + " id_terrain integer not null,"
                        + " id_type_jeu integer not null,"
                        + " primary key (id_terrain, id_type_jeu)"
                        + ")");

                // ----- Table matchs (avec type de jeu optionnel) -----
                st.executeUpdate("create table matchs ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " ronde_id integer not null,"
                        + " terrain_id integer,"
                        + " id_type_jeu integer,"  // Extension 4-5
                        + " score_e1 integer not null,"
                        + " score_e2 integer not null,"
                        + " statut varchar(20) not null"
                        + ")");

                // ----- Table equipe -----
                st.executeUpdate("create table equipe ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " id_match integer not null,"
                        + " numero integer not null,"
                        + " score integer not null"
                        + ")");

                // ----- Table match_joueur -----
                st.executeUpdate("create table match_joueur ( "
                        + " id_match integer not null,"
                        + " id_joueur integer not null,"
                        + " numero_equipe integer not null,"
                        + " primary key (id_match, id_joueur)"
                        + ")");

                // ========== EXTENSION 2 : MULTI-TOURNOI ==========

                // ----- Table inscription_tournoi -----
                st.executeUpdate("create table inscription_tournoi ( "
                        + " id_tournoi integer not null,"
                        + " id_joueur integer not null,"
                        + " date_inscription timestamp default current_timestamp,"
                        + " primary key (id_tournoi, id_joueur)"
                        + ")");

                // ----- Table classement_global -----
                st.executeUpdate("create table classement_global ( "
                        + " id_joueur integer primary key,"
                        + " nb_tournois_participes integer default 0,"
                        + " nb_matchs_joues integer default 0,"
                        + " nb_victoires integer default 0,"
                        + " score_total integer default 0,"
                        + " derniere_mise_a_jour timestamp default current_timestamp"
                        + ")");

                // ========== EXTENSION 6 : TEMPLATES ==========

                // ----- Table template_tournoi -----
                st.executeUpdate("create table template_tournoi ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "id") + ","
                        + " nom varchar(200) not null,"
                        + " description text,"
                        + " nb_terrains integer not null,"
                        + " nb_joueurs_par_equipe integer not null,"
                        + " duree_ronde_minutes integer default 20,"
                        + " public boolean default false,"
                        + " date_creation timestamp default current_timestamp"
                        + ")");

                // ----- Association template <-> terrain -----
                st.executeUpdate("create table template_terrain ( "
                        + " id_template integer not null,"
                        + " id_terrain integer not null,"
                        + " primary key (id_template, id_terrain)"
                        + ")");

                // ----- Association template <-> type_jeu -----
                st.executeUpdate("create table template_type_jeu ( "
                        + " id_template integer not null,"
                        + " id_type_jeu integer not null,"
                        + " primary key (id_template, id_type_jeu)"
                        + ")");

                // ========== CLES ETRANGERES ==========

                // Ronde
                st.executeUpdate("alter table ronde "
                        + " add constraint fk_ronde_tournoi "
                        + " foreign key (id_tournoi) references tournoi(id)");

                // Matchs
                st.executeUpdate("alter table matchs "
                        + " add constraint fk_match_ronde "
                        + " foreign key (ronde_id) references ronde(id)");

                st.executeUpdate("alter table matchs "
                        + " add constraint fk_match_terrain "
                        + " foreign key (terrain_id) references terrain(id)");

                st.executeUpdate("alter table matchs "
                        + " add constraint fk_match_type_jeu "
                        + " foreign key (id_type_jeu) references type_jeu(id)");

                // Equipe
                st.executeUpdate("alter table equipe "
                        + " add constraint fk_equipe_match "
                        + " foreign key (id_match) references matchs(id)");

                // Match_joueur
                st.executeUpdate("alter table match_joueur "
                        + " add constraint fk_mj_match "
                        + " foreign key (id_match) references matchs(id)");

                st.executeUpdate("alter table match_joueur "
                        + " add constraint fk_mj_joueur "
                        + " foreign key (id_joueur) references joueur(id)");

                // Inscription_tournoi (Extension 2)
                st.executeUpdate("alter table inscription_tournoi "
                        + " add constraint fk_inscription_tournoi "
                        + " foreign key (id_tournoi) references tournoi(id)");

                st.executeUpdate("alter table inscription_tournoi "
                        + " add constraint fk_inscription_joueur "
                        + " foreign key (id_joueur) references joueur(id)");

                // Classement_global (Extension 2)
                st.executeUpdate("alter table classement_global "
                        + " add constraint fk_classement_joueur "
                        + " foreign key (id_joueur) references joueur(id)");

                // Terrain_type_jeu (Extension 5)
                st.executeUpdate("alter table terrain_type_jeu "
                        + " add constraint fk_terrain_tj_terrain "
                        + " foreign key (id_terrain) references terrain(id)");

                st.executeUpdate("alter table terrain_type_jeu "
                        + " add constraint fk_terrain_tj_type "
                        + " foreign key (id_type_jeu) references type_jeu(id)");

                // Template_terrain (Extension 6)
                st.executeUpdate("alter table template_terrain "
                        + " add constraint fk_template_terrain_template "
                        + " foreign key (id_template) references template_tournoi(id)");

                st.executeUpdate("alter table template_terrain "
                        + " add constraint fk_template_terrain_terrain "
                        + " foreign key (id_terrain) references terrain(id)");

                // Template_type_jeu (Extension 6)
                st.executeUpdate("alter table template_type_jeu "
                        + " add constraint fk_template_tj_template "
                        + " foreign key (id_template) references template_tournoi(id)");

                st.executeUpdate("alter table template_type_jeu "
                        + " add constraint fk_template_tj_type "
                        + " foreign key (id_type_jeu) references type_jeu(id)");

                con.commit();
                System.out.println("✅ Schéma complet créé avec succès (base + extensions)");
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

            System.out.println("🗑️  Suppression des contraintes...");

            // Contraintes Extensions 6 (Templates)
            try { st.executeUpdate("alter table template_type_jeu drop constraint fk_template_tj_type"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table template_type_jeu drop constraint fk_template_tj_template"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table template_terrain drop constraint fk_template_terrain_terrain"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table template_terrain drop constraint fk_template_terrain_template"); } 
            catch (SQLException ex) {}

            // Contraintes Extensions 4-5 (Types de jeu)
            try { st.executeUpdate("alter table terrain_type_jeu drop constraint fk_terrain_tj_type"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table terrain_type_jeu drop constraint fk_terrain_tj_terrain"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table matchs drop constraint fk_match_type_jeu"); } 
            catch (SQLException ex) {}

            // Contraintes Extension 2 (Multi-tournoi)
            try { st.executeUpdate("alter table classement_global drop constraint fk_classement_joueur"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table inscription_tournoi drop constraint fk_inscription_joueur"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table inscription_tournoi drop constraint fk_inscription_tournoi"); } 
            catch (SQLException ex) {}

            // Contraintes de base
            try { st.executeUpdate("alter table match_joueur drop constraint fk_mj_joueur"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table match_joueur drop constraint fk_mj_match"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table equipe drop constraint fk_equipe_match"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table matchs drop constraint fk_match_terrain"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table matchs drop constraint fk_match_ronde"); } 
            catch (SQLException ex) {}
            try { st.executeUpdate("alter table ronde drop constraint fk_ronde_tournoi"); } 
            catch (SQLException ex) {}

            System.out.println("🗑️  Suppression des tables...");

            // Tables Extensions
            try { st.executeUpdate("drop table template_type_jeu"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table template_terrain"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table template_tournoi"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table terrain_type_jeu"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table type_jeu"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table classement_global"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table inscription_tournoi"); } catch (SQLException ex) {}

            // Tables de base
            try { st.executeUpdate("drop table match_joueur"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table equipe"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table matchs"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table ronde"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table terrain"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table joueur"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table tournoi"); } catch (SQLException ex) {}
            try { st.executeUpdate("drop table utilisateur"); } catch (SQLException ex) {}

            System.out.println("✅ Schéma supprimé");
        }
    }

    // ================== RAZ BDD (DROP + CREATE) ==================

    public static void razBdd(Connection con) throws SQLException {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║     RAZ COMPLETE DE LA BASE DE DONNEES    ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        deleteSchema(con);
        creeSchema(con);

        System.out.println("\n✅ Base de données réinitialisée avec succès !");
    }

    // ================== MAIN DE TEST ==================

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            razBdd(con);
            
            System.out.println("\n📋 Tables créées :");
            System.out.println("   ✓ utilisateur");
            System.out.println("   ✓ joueur");
            System.out.println("   ✓ tournoi (avec extensions multi-tournoi)");
            System.out.println("   ✓ terrain (avec extension plan)");
            System.out.println("   ✓ ronde");
            System.out.println("   ✓ matchs (avec type_jeu)");
            System.out.println("   ✓ equipe");
            System.out.println("   ✓ match_joueur");
            System.out.println("\n📋 Tables extensions :");
            System.out.println("   ✓ inscription_tournoi (Ext 2)");
            System.out.println("   ✓ classement_global (Ext 2)");
            System.out.println("   ✓ type_jeu (Ext 4-5)");
            System.out.println("   ✓ terrain_type_jeu (Ext 5)");
            System.out.println("   ✓ template_tournoi (Ext 6)");
            System.out.println("   ✓ template_terrain (Ext 6)");
            System.out.println("   ✓ template_type_jeu (Ext 6)");
            
        } catch (SQLException ex) {
            System.err.println("❌ Erreur : " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}