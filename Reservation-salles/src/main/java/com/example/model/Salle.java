package com.example.model;

import javax.persistence.*;

@Entity
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String Nom ;
    private int capacite ;
    // tes autres champs (nom, capacite, etc.)
    private String description;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Salle(int capacite, String name, Long id) {
        this.capacite = capacite;
        this.Nom = name;
        this.id = id;
    }

    public Salle() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return Nom;
    }

    public void setNom(String Nom) {
        this.Nom = Nom;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }


}
