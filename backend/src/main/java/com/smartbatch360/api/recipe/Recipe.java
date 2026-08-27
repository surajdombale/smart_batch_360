package com.smartbatch360.api.recipe;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A concrete mix recipe: name/grade (e.g. "M25"), description, and a list of
 * materials with quantities (lines owned/cascaded by the recipe - they only
 * ever exist as part of one). Built as a prerequisite for Production, which
 * references a recipe (user request, 2026-08-23).
 *
 * Since 2026-08-27 the total batch quantity is DERIVED from the material list
 * (see {@link #recalculateTotalBatchQuantity()}) rather than typed in, so it
 * cannot drift from the actual mix.
 */
@Entity
@Table(name = "recipe")
public class Recipe {

    /** Matches the DECIMAL(12,4) column the total is persisted in. */
    private static final int TOTAL_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "total_batch_quantity_m3", nullable = false, precision = 12, scale = 4)
    private BigDecimal totalBatchQuantityM3;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecipeStatus status;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<RecipeMaterial> materials = new ArrayList<>();

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

    public BigDecimal getTotalBatchQuantityM3() {
        return totalBatchQuantityM3;
    }

    /**
     * Only for the migration-era path where an existing recipe's stored total
     * is preserved as-is. New/edited recipes go through
     * {@link #recalculateTotalBatchQuantity()} instead.
     */
    public void setTotalBatchQuantityM3(BigDecimal totalBatchQuantityM3) {
        this.totalBatchQuantityM3 = totalBatchQuantityM3;
    }

    /**
     * Sums every material line's volume contribution. Rounded once, at the end,
     * to the persisted scale - the per-material conversions deliberately keep
     * more precision than that so the rounding doesn't compound.
     *
     * @throws IllegalStateException if any material is a weight with no density set
     */
    public void recalculateTotalBatchQuantity() {
        BigDecimal total = BigDecimal.ZERO;
        for (RecipeMaterial material : materials) {
            total = total.add(material.toCubicMetres());
        }
        this.totalBatchQuantityM3 = total.setScale(TOTAL_SCALE, RoundingMode.HALF_UP);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RecipeStatus getStatus() {
        return status;
    }

    public void setStatus(RecipeStatus status) {
        this.status = status;
    }

    public List<RecipeMaterial> getMaterials() {
        return materials;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
