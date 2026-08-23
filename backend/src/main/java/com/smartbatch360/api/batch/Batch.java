package com.smartbatch360.api.batch;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.vehicle.Vehicle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A single production batch - Production's "Batch Details" / "Current Batch"
 * panel (docs/02_UI_REFERENCE.md), persisted as real history rather than a
 * one-off screen, per the scope confirmed with the user (2026-08-23).
 * Equipment status and overall status are manual/simulated - no PLC
 * integration (intentionally postponed).
 */
@Entity
@Table(name = "batch")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false, length = 30, unique = true)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "target_quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal targetQuantity;

    @Column(name = "produced_quantity", nullable = false, precision = 8, scale = 2)
    private BigDecimal producedQuantity = BigDecimal.ZERO;

    @Column(name = "cycle_date_time", nullable = false)
    private Instant cycleDateTime;

    @Column(name = "cycle_number")
    private Integer cycleNumber;

    @Column(name = "shift", length = 50)
    private String shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "mixer_status", nullable = false, length = 20)
    private EquipmentStatus mixerStatus = EquipmentStatus.STOPPED;

    @Enumerated(EnumType.STRING)
    @Column(name = "conveyor_status", nullable = false, length = 20)
    private EquipmentStatus conveyorStatus = EquipmentStatus.STOPPED;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_valve_status", nullable = false, length = 20)
    private EquipmentStatus waterValveStatus = EquipmentStatus.STOPPED;

    @Enumerated(EnumType.STRING)
    @Column(name = "cement_screw_status", nullable = false, length = 20)
    private EquipmentStatus cementScrewStatus = EquipmentStatus.STOPPED;

    @Enumerated(EnumType.STRING)
    @Column(name = "compressor_status", nullable = false, length = 20)
    private EquipmentStatus compressorStatus = EquipmentStatus.STOPPED;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<BatchMaterial> materials = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (cycleDateTime == null) {
            cycleDateTime = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public BigDecimal getTargetQuantity() {
        return targetQuantity;
    }

    public void setTargetQuantity(BigDecimal targetQuantity) {
        this.targetQuantity = targetQuantity;
    }

    public BigDecimal getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(BigDecimal producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    public Instant getCycleDateTime() {
        return cycleDateTime;
    }

    public void setCycleDateTime(Instant cycleDateTime) {
        this.cycleDateTime = cycleDateTime;
    }

    public Integer getCycleNumber() {
        return cycleNumber;
    }

    public void setCycleNumber(Integer cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public EquipmentStatus getMixerStatus() {
        return mixerStatus;
    }

    public void setMixerStatus(EquipmentStatus mixerStatus) {
        this.mixerStatus = mixerStatus;
    }

    public EquipmentStatus getConveyorStatus() {
        return conveyorStatus;
    }

    public void setConveyorStatus(EquipmentStatus conveyorStatus) {
        this.conveyorStatus = conveyorStatus;
    }

    public EquipmentStatus getWaterValveStatus() {
        return waterValveStatus;
    }

    public void setWaterValveStatus(EquipmentStatus waterValveStatus) {
        this.waterValveStatus = waterValveStatus;
    }

    public EquipmentStatus getCementScrewStatus() {
        return cementScrewStatus;
    }

    public void setCementScrewStatus(EquipmentStatus cementScrewStatus) {
        this.cementScrewStatus = cementScrewStatus;
    }

    public EquipmentStatus getCompressorStatus() {
        return compressorStatus;
    }

    public void setCompressorStatus(EquipmentStatus compressorStatus) {
        this.compressorStatus = compressorStatus;
    }

    public List<BatchMaterial> getMaterials() {
        return materials;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
