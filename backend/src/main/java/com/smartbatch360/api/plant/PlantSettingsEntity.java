package com.smartbatch360.api.plant;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The plant's own figures, under Settings > Plant Details.
 *
 * Exactly one row, id 1. There is one plant, and a second row would make "the
 * mixer capacity" a question with two answers.
 */
@Entity
@Table(name = "plant_settings")
public class PlantSettingsEntity {

    /** The only id this table ever has. */
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "supervisor_name", length = 150)
    private String supervisorName;

    @Column(name = "mixer_capacity_m3", precision = 4, scale = 2)
    private BigDecimal mixerCapacityM3;

    /** What the plant can turn out in an hour, in cubic metres. */
    @Column(name = "plant_capacity_m3_per_hour", precision = 6, scale = 2)
    private BigDecimal plantCapacityM3PerHour;

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

    public String getSupervisorName() {
        return supervisorName;
    }

    public void setSupervisorName(String supervisorName) {
        this.supervisorName = supervisorName;
    }

    public BigDecimal getMixerCapacityM3() {
        return mixerCapacityM3;
    }

    public void setMixerCapacityM3(BigDecimal mixerCapacityM3) {
        this.mixerCapacityM3 = mixerCapacityM3;
    }

    public BigDecimal getPlantCapacityM3PerHour() {
        return plantCapacityM3PerHour;
    }

    public void setPlantCapacityM3PerHour(BigDecimal plantCapacityM3PerHour) {
        this.plantCapacityM3PerHour = plantCapacityM3PerHour;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
