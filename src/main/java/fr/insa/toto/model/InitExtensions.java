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
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * Initialisation / RAZ base + données de test (version sûre)
 * Objectif : compiler et permettre InitExtensions.razComplete(con)
 * sans dépendre de méthodes non implémentées (demarrer/inscrireJoueur/terminer...).
 */
public class InitExtensions {

    /**
     * Supprime toutes les tables (base + extensions) si elles existent.
     * L'ordre est important à cause des clés étrangères.
     */
    public static void supprimerTout(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {

            // Extensions (si tu les as)
            dropQuiet(st, "template_type_jeu");
            dropQuiet(st, "template_terrain");
            dropQuiet(st, "template_tournoi");
            dropQuiet(st, "terrain_type_jeu");
            dropQuiet(st, "type_jeu");
            dropQuiet(st, "classement_global");
            dropQuiet(st, "inscription_tournoi");

            // Base
            dropQuiet(st, "match_joueur");
            dropQuiet(st, "equipe");
            dropQuiet(st, "matchs");
            dropQuiet(st, "ronde");
            dropQuiet(st, "terrain");
            dropQuiet(st, "joueur");
            dropQuiet(st, "tournoi");
            dropQuiet(st, "utilisateur");
        }
    }

    private static void dropQuiet(Statement st, String table) {
        try {
            st.executeUpdate("DROP TABLE IF EXISTS " + table);
        } catch (SQLException ignored) {
            // on ignore volontairement
        }
    }

    /**
     * Crée le schéma de base.
     * (Tu peux ensuite ajouter des appels aux schémas d'extensions si tu es sûr que les méthodes existent)
     */
    public static void creerSchemaComplet(Connection con) throws SQLException {
        // Schéma de base (doit exister dans ton projet)
        GestionBDD.creeSchema(con);

        // Si plus tard tu veux activer tes extensions, tu peux décommenter
        // seulement si ces méthodes existent réellement dans ton code.
        //
        // TournoiMulti.creerSchemaMultiTournoi(con);
        // TerrainAvecPlan.ajouterColonnesPlan(con);
        // TypeJeu.creerSchemTypeJeu(con);
        // TemplateTournoi.creerSchemaTemplate(con);
    }

    /**
     * Données de test simples (sans dépendre d'API manquantes).
     */
    public static void creerDonneesTest(Connection con) throws SQLException {

        // Utilisateurs
        Utilisateur admin = new Utilisateur("admin", "admin", 1);
        admin.saveInDB(con);

        Utilisateur user1 = new Utilisateur("alice", "pass", 2);
        user1.saveInDB(con);

        Utilisateur user2 = new Utilisateur("bob", "pass", 2);
        user2.saveInDB(con);

        // Joueurs (si ton constructeur Joueur correspond)
        List<Joueur> joueurs = List.of(
                new Joueur("alice", "Avancé", 170, "Durand", "Alice", "F", LocalDate.of(1995, 3, 15)),
                new Joueur("bob", "Débutant", 180, "Martin", "Bob", "M", LocalDate.of(1998, 7, 22))
        );

        for (Joueur j : joueurs) {
            j.saveInDB(con);
        }

        // Terrains simples (si ton modèle Terrain le permet)
        // Si tu as TerrainAvecPlan, tu peux remplacer par TerrainAvecPlan.
        // Exemple (à adapter si tu n’as pas ce constructeur) :
        // Terrain t1 = new Terrain("Court 1", "Terrain principal");
        // t1.saveInDB(con);
    }

    /**
     * RAZ complète : supprime tout puis recrée schéma + quelques données.
     */
    public static void razComplete(Connection con) throws SQLException {
        supprimerTout(con);
        creerSchemaComplet(con);
        creerDonneesTest(con);
    }

    /**
     * Main de test (optionnel)
     */
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            razComplete(con);
            System.out.println("✅ RAZ complète terminée");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
