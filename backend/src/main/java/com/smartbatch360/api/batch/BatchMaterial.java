package com.smartbatch360.api.batch;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** One material line of a Batch's consumption (target/setpoint/achieved), same shape as RecipeMaterial plus setpoint/achieved. */
@Entity
@Table(name = "batch_material")
public class BatchMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "target", nullable = false, precision = 8, scale = 2)
    private BigDecimal target;

    @Column(name = "setpoint", nullable = false, precision = 8, scale = 2)
    private BigDecimal setpoint;

    @Column(name = "achieved", nullable = false, precision = 8, scale = 2)
    private BigDecimal achieved;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public Long getId() {
        return id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public void setTarget(BigDecimal target) {
        this.target = target;
    }

    public BigDecimal getSetpoint() {
        return setpoint;
    }

    public void setSetpoint(BigDecimal setpoint) {
        this.setpoint = setpoint;
    }

    public BigDecimal getAchieved() {
        return achieved;
    }

    public void setAchieved(BigDecimal achieved) {
        this.achieved = achieved;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
