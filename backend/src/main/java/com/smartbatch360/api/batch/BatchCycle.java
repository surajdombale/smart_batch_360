package com.smartbatch360.api.batch;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One mixer cycle of a batch, as the PLC reported it: when it ran, and what
 * each material actually weighed.
 */
@BatchSize(size = 100)
@Entity
@Table(name = "batch_cycle")
public class BatchCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber;

    /** When the plant ran the cycle - not when the PLC got round to telling us. */
    @Column(name = "cycle_time", nullable = false)
    private Instant cycleTime;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 100)
    private List<BatchCycleMaterial> materials = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Set here rather than left to the column default: the tests build their
    // schema from these entities, where that default does not exist. Batch does
    // the same.
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

    /** What this cycle weighed in total - the report's row total. */
    public BigDecimal totalAchieved() {
        BigDecimal total = BigDecimal.ZERO;
        for (BatchCycleMaterial material : materials) {
            total = total.add(material.getAchieved());
        }
        return total;
    }

    public Long getId() {
        return id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Integer getCycleNumber() {
        return cycleNumber;
    }

    public void setCycleNumber(Integer cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public Instant getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(Instant cycleTime) {
        this.cycleTime = cycleTime;
    }

    public List<BatchCycleMaterial> getMaterials() {
        return materials;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
