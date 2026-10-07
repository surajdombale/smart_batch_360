package com.smartbatch360.api.batch;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the plant will actually run for a load: how many mixer cycles, how much
 * each cycle takes, and what each material is set to per cycle.
 */
public record BatchPlan(
        BigDecimal batchSizeM3,
        BigDecimal totalKg,
        int cycles,
        BigDecimal perCycleM3,
        BigDecimal perCycleKg,
        List<MaterialSetpoint> materials
) {

    /**
     * One material's setpoint. The machine is given the per-cycle figure and
     * runs it that many times, so the total is the per-cycle value multiplied
     * out rather than the other way round - that is what the batch report's
     * "Set total" row adds up to.
     */
    public record MaterialSetpoint(
            String materialName,
            /** Straight from the recipe, for one recipe batch - what the mix asks for. */
            BigDecimal recipeQuantityKg,
            BigDecimal perCycleKg,
            BigDecimal totalKg) {
    }
}
