package com.smartbatch360.api.batch;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** What one material weighed in one cycle. */
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

    @Column(name = "achieved", nullable = false, precision = 8, scale = 2)
    private BigDecimal achieved;

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

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
