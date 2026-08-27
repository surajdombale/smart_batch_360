package com.smartbatch360.desktop.material;

/** Mirrors the backend's MaterialUnit. KG is weight-based and needs a density to convert to m3. */
public enum MaterialUnit {

    KG(true),
    LITRE(false);

    private final boolean requiresDensity;

    MaterialUnit(boolean requiresDensity) {
        this.requiresDensity = requiresDensity;
    }

    public boolean requiresDensity() {
        return requiresDensity;
    }
}
