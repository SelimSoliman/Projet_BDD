package fr.insa.toto.model;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MatchAvecTypeJeu extends Match {

    private TypeJeu typeJeu;
    private List<Equipe> equipes;  // Peut contenir plus de 2 équipes

    public MatchAvecTypeJeu(Ronde ronde, Terrain terrain, TypeJeu typeJeu) {
        super(ronde, terrain);
        this.typeJeu = typeJeu;
        this.equipes = new ArrayList<>();
        
        // Créer le nombre d'équipes nécessaire
        for (int i = 1; i <= typeJeu.getNbEquipes(); i++) {
            equipes.add(new Equipe(this, i));
        }
    }

    /**
     * Sauvegarde le match avec son type de jeu
     */
    @Override
    protected PreparedStatement saveSansId(Connection con) throws SQLException {
        String sql = """
            INSERT INTO matchs (ronde_id, terrain_id, id_type_jeu, score_e1, score_e2, statut)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, getRonde().getId());
        ps.setInt(2, getTerrain() != null ? getTerrain().getId() : 0);
        ps.setInt(3, typeJeu.getId());
        ps.setInt(4, getScoreEquipe1());
        ps.setInt(5, getScoreEquipe2());
        ps.setString(6, getStatut().name());
        ps.executeUpdate();
        return ps;
    }

    public List<Equipe> getToutesLesEquipes() {
        return new ArrayList<>(equipes);
    }

    public TypeJeu getTypeJeu() {
        return typeJeu;
    }

    /**
     * Valide que toutes les équipes ont la bonne taille
     */
    public boolean validerTaillesEquipes() {
        return typeJeu.validerRepartitionJoueurs(
            equipes.stream()
                .map(Equipe::getJoueurs)
                .toList()
        );
    }
}