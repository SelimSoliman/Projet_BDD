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
public class Terrain {
  
    private int id;
    private String nom;
    private boolean disponible;
    
    public Terrain(int id, String nom) {
        this.id = id;
        this.nom = nom;
        this.disponible = true;
    }
    
    
    public int getId() {
        return id;
    }
    
    public String getNom() {
        return nom;
    }
    
    public boolean estDisponible() {
        return disponible;
    }
    
    // Setters
    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID doit être positif");
        }
        this.id = id;
    }
    
    public void setNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }
        this.nom = nom;
    }
    
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
    
    // Méthodes métier
    public void occuper() {
        if (!disponible) {
            throw new IllegalStateException("Le terrain est déjà occupé");
        }
        this.disponible = false;
    }
    
    public void liberer() {
        if (disponible) {
            throw new IllegalStateException("Le terrain est déjà libre");
        }
        this.disponible = true;
    }
    
    public void basculerDisponibilite() {
        this.disponible = !this.disponible;
    }
    
    @Override
    public String toString() {
        return "Terrain " + id + " : " + nom + 
               " - " + (disponible ? "Disponible" : "Occupé");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Terrain terrain = (Terrain) obj;
        return id == terrain.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
  

