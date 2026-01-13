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

import fr.insa.beuvron.utils.ConsoleFdB;
import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author francois
 */
public class Utilisateur extends ClasseMiroir implements Serializable {

    private static final long serialVersionUID = 1L;

    // Constantes pour les rôles
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_USER = 2;
    public static final int ROLE_PLAYER = 3; // ✅ AJOUT

    private String surnom;
    private String pass;
    private int role;

    // ✅ AJOUT : lien optionnel vers un joueur (null si pas joueur)
    private Integer idJoueur;

    /**
     * pour nouvel utilisateur en mémoire
     */
    public Utilisateur(String surnom, String pass, int role) {
        super();
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
        this.idJoueur = null; // ✅ AJOUT
    }

    /**
     * pour utilisateur récupéré de la base de données
     */
    public Utilisateur(int id, String surnom, String pass, int role) {
        super(id);
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
        this.idJoueur = null; // ✅ AJOUT (pour compat)
    }

    // ✅ AJOUT : constructeur complet avec idJoueur (utile quand on lit la BD)
    public Utilisateur(int id, String surnom, String pass, int role, Integer idJoueur) {
        super(id);
        this.surnom = surnom;
        this.pass = pass;
        this.role = role;
        this.idJoueur = idJoueur;
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "insert into utilisateur (surnom,pass,role,id_joueur) values (?,?,?,?)", // ✅ MODIF
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setString(1, this.getSurnom());
        insert.setString(2, this.getPass());
        insert.setInt(3, getRole());
        insert.setObject(4, this.getIdJoueur()); // ✅ AJOUT (null autorisé)
        insert.executeUpdate();
        return insert;
    }

    public static List<Utilisateur> tousLesUtilisateur(Connection con) throws SQLException {
        List<Utilisateur> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("select id,surnom,pass,role,id_joueur from utilisateur")) { // ✅ MODIF
            try (ResultSet allU = pst.executeQuery()) {
                while (allU.next()) {
                    res.add(new Utilisateur(
                            allU.getInt("id"),
                            allU.getString("surnom"),
                            allU.getString("pass"),
                            allU.getInt("role"),
                            (Integer) allU.getObject("id_joueur") // ✅ AJOUT
                    ));
                }
            }
        }
        return res;
    }

    public static Optional<Utilisateur> findBySurnomPass(Connection con, String surnom, String pass) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "select id,role,id_joueur from utilisateur where surnom = ? and pass = ?")) { // ✅ MODIF
            pst.setString(1, surnom);
            pst.setString(2, pass);
            ResultSet res = pst.executeQuery();
            if (res.next()) {
                int id = res.getInt("id");
                int role = res.getInt("role");
                Integer idJoueur = (Integer) res.getObject("id_joueur"); // ✅ AJOUT
                return Optional.of(new Utilisateur(id, surnom, pass, role, idJoueur));
            } else {
                return Optional.empty();
            }
        }
    }

    /**
     * supprime l'utilisateur de la BdD. Attention : supprime d'abord les
     * éventuelles dépendances.
     *
     * @param con
     * @throws SQLException
     */
    public void deleteInDB(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new ClasseMiroir.EntiteNonSauvegardee();
        }
        try {
            con.setAutoCommit(false);
            try (PreparedStatement pst = con.prepareStatement(
                    "delete from pratique where idutilisateur = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            try (PreparedStatement pst = con.prepareStatement(
                    "delete from apprecie where u1 = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            try (PreparedStatement pst = con.prepareStatement(
                    "delete from apprecie where u2 = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }

            try (PreparedStatement pst = con.prepareStatement(
                    "delete from utilisateur where id = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            this.entiteSupprimee();
            con.commit();
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    public static Utilisateur entreeConsole() {
        String nom = ConsoleFdB.entreeString("surnom de l'utilisateur : ");
        String pass = ConsoleFdB.entreeString("password : ");
        return new Utilisateur(nom, pass, ROLE_USER);
    }

    public static List<Utilisateur> findAll(Connection con) throws SQLException {
        List<Utilisateur> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement(
                "select id, surnom, pass, role, id_joueur from UTILISATEUR")) { // ✅ MODIF
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String surnom = rs.getString("surnom");
                    String pass = rs.getString("pass");
                    int role = rs.getInt("role");
                    Integer idJoueur = (Integer) rs.getObject("id_joueur"); // ✅ AJOUT
                    res.add(new Utilisateur(id, surnom, pass, role, idJoueur));
                }
            }
        }
        return res;
    }

    // ========== GETTERS / SETTERS ==========

    /**
     * @return the surnom
     */
    public String getSurnom() {
        return surnom;
    }

    /**
     * @param surnom the surnom to set
     */
    public void setSurnom(String surnom) {
        this.surnom = surnom;
    }

    /**
     * @return the pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * @param pass the pass to set
     */
    public void setPass(String pass) {
        this.pass = pass;
    }

    /**
     * @return the role
     */
    public int getRole() {
        return role;
    }

    /**
     * @param role the role to set
     */
    public void setRole(int role) {
        this.role = role;
    }

    // ✅ AJOUT : setter utile quand tu crées le compte joueur
    public void setIdJoueur(Integer idJoueur) {
        this.idJoueur = idJoueur;
    }

    /**
     * Vérifie si l'utilisateur est administrateur.
     * @return true si role == ROLE_ADMIN (1), false sinon
     */
    public boolean isAdmin() {
        return this.role == ROLE_ADMIN;
    }

    // ✅ tu l'avais : maintenant ça compile car ROLE_PLAYER existe
    public boolean isPlayer() {
        return this.role == ROLE_PLAYER;
    }

    // ✅ tu l'avais : maintenant ça compile car idJoueur existe
    public Integer getIdJoueur() {
        return idJoueur;
    }

    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + getId() +
                ", surnom='" + surnom + '\'' +
                ", role=" + (isAdmin() ? "ADMIN" : (isPlayer() ? "PLAYER" : "USER")) +
                '}';
    }
}
