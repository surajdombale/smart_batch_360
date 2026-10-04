package com.smartbatch360.api.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * What a cubic metre of concrete weighs, in kilograms.
 *
 * Production is planned in m3 - a load has to be matched to a vehicle, and
 * vehicle capacity is in m3 - while materials and recipes are in kilograms.
 * This is the one number that joins them.
 *
 * It is NOT a guess. The project's standing rule is that no conversion factor
 * is ever assumed, and this one was given by the user on 04-Oct-2026:
 * "2400kg = 1 m3 for Concrete". It is configurable with
 * smartbatch360.plant.concrete-density-kg-per-m3 so a plant working to a
 * different mix density can say so without a code change.
 */
@Component
public class ConcreteDensity {

    /** The figure the user gave; see the class note before changing it. */
    static final BigDecimal DEFAULT_KG_PER_M3 = new BigDecimal("2400");

    private final BigDecimal kgPerCubicMetre;

    public ConcreteDensity(@Value("${smartbatch360.plant.concrete-density-kg-per-m3:}") String configured) {
        if (configured == null || configured.isBlank()) {
            this.kgPerCubicMetre = DEFAULT_KG_PER_M3;
            return;
        }
        BigDecimal value = new BigDecimal(configured.trim());
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Concrete density must be greater than zero, but is "
                    + value.toPlainString() + ".");
        }
        this.kgPerCubicMetre = value;
    }

    public static ConcreteDensity of(String kgPerCubicMetre) {
        return new ConcreteDensity(kgPerCubicMetre);
    }

    public static ConcreteDensity standard() {
        return new ConcreteDensity("");
    }

    public BigDecimal kgPerCubicMetre() {
        return kgPerCubicMetre;
    }

    /** What a load of this many cubic metres weighs. */
    public BigDecimal toKilograms(BigDecimal cubicMetres) {
        return cubicMetres.multiply(kgPerCubicMetre);
    }

    /** What this many kilograms occupies - the figure a vehicle is matched against. */
    public BigDecimal toCubicMetres(BigDecimal kilograms) {
        return kilograms.divide(kgPerCubicMetre, 4, java.math.RoundingMode.HALF_UP);
    }
}
