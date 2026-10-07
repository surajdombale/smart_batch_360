package com.smartbatch360.api.batch;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** What one material was aiming at in one cycle, and what it weighed. */
@Entity
@Table(name = "batch_cycle_material")
public class BatchCycleMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_cycle_id", nullable = false)
    private BatchCycle cycle;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    /**
     * What the cycle was aiming at for this material. Null only where neither
     * the PLC nor the batch's own plan could say.
     */
    @Column(name = "target", precision = 8, scale = 2)
    private BigDecimal target;

    @Column(name = "setpoint", precision = 8, scale = 2)
    private BigDecimal setpoint;

    @Column(name = "achieved", nullable = false, precision = 8, scale = 2)
    private BigDecimal achieved;

    /** Reported for aggregates when moisture correction is on. */
    @Column(name = "moisture_content", precision = 6, scale = 2)
    private BigDecimal moistureContent;

    @Column(name = "absorption", precision = 6, scale = 2)
    private BigDecimal absorption;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public Long getId() {
        return id;
    }

    public BatchCycle getCycle() {
        return cycle;
    }

    public void setCycle(BatchCycle cycle) {
        this.cycle = cycle;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public BigDecimal getAchieved() {
        return achieved;
    }

    public void setAchieved(BigDecimal achieved) {
        this.achieved = achieved;
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

    public BigDecimal getMoistureContent() {
        return moistureContent;
    }

    public void setMoistureContent(BigDecimal moistureContent) {
        this.moistureContent = moistureContent;
    }

    public BigDecimal getAbsorption() {
        return absorption;
    }

    public void setAbsorption(BigDecimal absorption) {
        this.absorption = absorption;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
