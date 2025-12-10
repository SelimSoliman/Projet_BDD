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
import java.util.List;

public class Match {

    public enum Statut {
        EN_COURS,
        CLOS
    }

    private int id;                    // Identifiant BD éventuel
    private Ronde ronde;               // Ronde à laquelle appartient ce match

    private List<Joueur> equipe1;      // Liste des joueurs de l'équipe 1
    private List<Joueur> equipe2;      // Liste des joueurs de l'équipe 2

    private int scoreEquipe1 = 0;
    private int scoreEquipe2 = 0;

    private Statut statut = Statut.EN_COURS;

    public Match(int id, Ronde ronde, List<Joueur> equipe1, List<Joueur> equipe2) {
        this.id = id;
        this.ronde = ronde;
        this.equipe1 = equipe1;
        this.equipe2 = equipe2;
    }

    // ---------------- GETTERS / SETTERS ----------------

    public int getId() { return id; }

    public List<Joueur> getEquipe1() { return equipe1; }

    public List<Joueur> getEquipe2() { return equipe2; }

    public int getScoreEquipe1() { return scoreEquipe1; }

    public int getScoreEquipe2() { return scoreEquipe2; }

    public Statut getStatut() { return statut; }

    public Ronde getRonde() { return ronde; }

    // ---------------- MÉTHODES PRINCIPALES ----------------

    public void definirScores(int score1, int score2) {
        if (statut == Statut.CLOS) {
            throw new IllegalStateException("Match déjà clos");
        }

        this.scoreEquipe1 = score1;
        this.scoreEquipe2 = score2;

        // Lorsqu'on définit les scores, on clot le match.
        cloreMatch();
    }

    private void cloreMatch() {
        this.statut = Statut.CLOS;

        // Chaque joueur d’une équipe reçoit le même score
        for (Joueur j : equipe1) {
            j.ajouterScore(scoreEquipe1);
        }
        for (Joueur j : equipe2) {
            j.ajouterScore(scoreEquipe2);
        }
    }

    @Override
    public String toString() {
        return "Match : E1=" + equipe1.size() + " joueurs, score=" + scoreEquipe1 +
               " | E2=" + equipe2.size() + " joueurs, score=" + scoreEquipe2 +
               " (" + statut + ")";
    }
}
