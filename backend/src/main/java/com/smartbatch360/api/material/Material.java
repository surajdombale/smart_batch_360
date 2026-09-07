package com.smartbatch360.api.material;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A material a recipe can be built from (Water, Cement, Sand, ...).
 *
 * Everything is measured in kilograms as of 2026-09-07. Materials used to
 * carry their own unit (KG or LITRE) and a density, so a recipe could work out
 * its batch size in cubic metres. That conversion caused more trouble than it
 * solved - a KG material was unusable until someone supplied a density, and
 * the plant weighs everything in kg anyway - so both are gone, along with the
 * conversion itself. A quantity of a material is a number of kilograms.
 */
@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
