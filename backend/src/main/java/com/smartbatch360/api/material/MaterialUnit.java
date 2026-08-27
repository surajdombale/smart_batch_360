package com.smartbatch360.api.material;

/**
 * How a material is measured. Recipes are totalled in m3, so each unit has to
 * be convertible to a volume:
 *  - LITRE converts exactly (1 m3 = 1000 L).
 *  - KG cannot be converted without knowing the material's density, which is
 *    why Material carries densityKgPerM3 (required for KG, unused for LITRE).
 */
public enum MaterialUnit {

    KG(true),
    LITRE(false);

    private final boolean requiresDensity;

    MaterialUnit(boolean requiresDensity) {
        this.requiresDensity = requiresDensity;
    }

    /** True when a quantity in this unit is a weight and needs a density to become a volume. */
    public boolean requiresDensity() {
        return requiresDensity;
    }
}
