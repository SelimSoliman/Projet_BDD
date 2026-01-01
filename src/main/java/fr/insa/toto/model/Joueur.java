package fr.insa.toto.model;

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Joueur extends ClasseMiroir {  

    // --- attributs BD principaux ---
    private String surnom;
    private String categorie;
    private int taillecm;

    // --- attributs "infos détaillées" suggérés par le sujet ---
    private String nom;
    private String prenom;
    private String sexe;              // ou un enum
    private LocalDate dateNaissance;

    // --- constructeur pour nouvel objet en mémoire ---
    public Joueur(String surnom, String categorie, int taillecm,
                  String nom, String prenom, String sexe, LocalDate dateNaissance) {
        super();                  // id = -1 géré par ClasseMiroir
        this.surnom = surnom;
        this.categorie = categorie;
        this.taillecm = taillecm;
        this.nom = nom;
        this.prenom = prenom;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
    }

    // --- constructeur depuis la BD ---
    public Joueur(int id, String surnom, String categorie, int taillecm,
                  String nom, String prenom, String sexe, LocalDate dateNaissance) {
        super(id);
        this.surnom = surnom;
        this.categorie = categorie;
        this.taillecm = taillecm;
        this.nom = nom;
        this.prenom = prenom;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
    }

    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO joueur
                (surnom, categorie, taillecm, nom, prenom, sexe, date_naissance)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, this.surnom);
        ps.setString(2, this.categorie);
        ps.setInt(3, this.taillecm);
        ps.setString(4, this.nom);
        ps.setString(5, this.prenom);
        ps.setString(6, this.sexe);
        ps.setDate(7, java.sql.Date.valueOf(this.dateNaissance));
        ps.executeUpdate();
        return ps;
    }

    public static List<Joueur> tousLesJoueurs(Connection con) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = """
            SELECT id, surnom, categorie, taillecm, nom, prenom, sexe, date_naissance
            FROM joueur
            """;
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Joueur j = new Joueur(
                        rs.getInt("id"),
                        rs.getString("surnom"),
                        rs.getString("categorie"),
                        rs.getInt("taillecm"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("sexe"),
                        rs.getDate("date_naissance").toLocalDate()
                );
                res.add(j);
            }
        }
        return res;
    }
public static void supprimer(Connection con, int id) throws SQLException {
    String sql = "delete from joueur where id = ?";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}

    // --- getters / setters pour tous les champs ---

    public String getSurnom() { return surnom; }
    public void setSurnom(String surnom) { this.surnom = surnom; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public int getTaillecm() { return taillecm; }
    public void setTaillecm(int taillecm) { this.taillecm = taillecm; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
}
