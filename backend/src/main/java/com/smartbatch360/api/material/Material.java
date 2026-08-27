package com.smartbatch360.api.material;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * A material a recipe can be built from (Water, Cement, Sand, ...), with the
 * unit it is measured in. Introduced 2026-08-27 so recipes reference real
 * material records instead of repeating a free-text name + unit on every row.
 */
@Entity
@Table(name = "material")
public class Material {

    /** Litres per cubic metre - exact, not an assumption. */
    private static final BigDecimal LITRES_PER_CUBIC_METRE = new BigDecimal("1000");

    /**
     * Intermediate conversion scale. Deliberately wider than the persisted
     * DECIMAL(12,4) so per-material rounding doesn't accumulate into the
     * recipe total before it is rounded once at the end.
     */
    private static final int CONVERSION_SCALE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private MaterialUnit unit;

    /**
     * Density in kg/m3. Required for KG materials (a weight can't become a
     * volume without it), unused for LITRE. Nullable because rows backfilled
     * from pre-existing recipe data have no known density yet - the service
     * enforces it on create/update rather than the schema.
     */
    @Column(name = "density_kg_per_m3", precision = 10, scale = 3)
    private BigDecimal densityKgPerM3;

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

    /** True when this material can currently be converted to a volume (KG materials need a density first). */
    public boolean isConvertibleToVolume() {
        return !unit.requiresDensity()
                || (densityKgPerM3 != null && densityKgPerM3.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Converts a quantity of this material, in this material's unit, to cubic
     * metres. Lives here rather than in a service so the rule travels with the
     * material itself - the recipe total is just a sum of these.
     *
     * @throws IllegalStateException if the material is a weight with no usable density
     */
    public BigDecimal toCubicMetres(BigDecimal quantity) {
        if (!isConvertibleToVolume()) {
            throw new IllegalStateException(
                    "Material '" + name + "' is measured in " + unit + " and needs a density before it can be "
                            + "converted to a volume.");
        }
        BigDecimal divisor = unit.requiresDensity() ? densityKgPerM3 : LITRES_PER_CUBIC_METRE;
        return quantity.divide(divisor, CONVERSION_SCALE, RoundingMode.HALF_UP);
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

    public MaterialUnit getUnit() {
        return unit;
    }

    public void setUnit(MaterialUnit unit) {
        this.unit = unit;
    }

    public BigDecimal getDensityKgPerM3() {
        return densityKgPerM3;
    }

    public void setDensityKgPerM3(BigDecimal densityKgPerM3) {
        this.densityKgPerM3 = densityKgPerM3;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
