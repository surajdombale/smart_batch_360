package com.smartbatch360.api.batch;

import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeMaterial;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns "I want this many cubic metres of this recipe" into what the plant runs.
 *
 * The chain is: the load's volume becomes a weight through the concrete density,
 * that weight is scaled against what the recipe makes, and the result is divided
 * across the mixer cycles the load needs. No per-material density is involved at
 * any point - the recipe's own proportions do that work.
 */
@Component
public class BatchPlanner {

    /** Matches the scale material quantities are stored at. */
    private static final int KG_SCALE = 2;

    /** Wide enough that scaling a recipe does not lose precision before rounding. */
    private static final int RATIO_SCALE = 10;

    private final ConcreteDensity concreteDensity;

    public BatchPlanner(ConcreteDensity concreteDensity) {
        this.concreteDensity = concreteDensity;
    }

    /**
     * The capacity is passed in rather than held: it is editable on Company
     * Details, so holding one at startup would plan against a mixer the plant
     * no longer has.
     */
    public BatchPlan plan(Recipe recipe, BigDecimal batchSizeM3, MixerCapacity mixerCapacity) {
        BigDecimal recipeTotal = recipe.getTotalBatchQuantityKg();
        if (recipeTotal == null || recipeTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Recipe '" + recipe.getName() + "' has no materials, so a batch of it "
                    + "cannot be planned.");
        }

        BatchCycles cycles = BatchCycles.plan(batchSizeM3, mixerCapacity);
        BigDecimal totalKg = concreteDensity.toKilograms(batchSizeM3);

        // How many recipe-batches this load is worth.
        BigDecimal scale = totalKg.divide(recipeTotal, RATIO_SCALE, RoundingMode.HALF_UP);

        List<BatchPlan.MaterialSetpoint> setpoints = new ArrayList<>();
        for (RecipeMaterial line : recipe.getMaterials()) {
            BigDecimal forWholeLoad = line.getQuantity().multiply(scale);
            BigDecimal perCycle = forWholeLoad.divide(BigDecimal.valueOf(cycles.cycles()), KG_SCALE,
                    RoundingMode.HALF_UP);
            // The machine runs the per-cycle figure, so the total is that
            // multiplied out. Deriving it the other way would leave the report's
            // "Set total" row disagreeing with its own cycle rows.
            BigDecimal total = perCycle.multiply(BigDecimal.valueOf(cycles.cycles()));
            setpoints.add(new BatchPlan.MaterialSetpoint(line.getMaterial().getName(),
                    line.getQuantity(), perCycle, total));
        }

        return new BatchPlan(
                batchSizeM3,
                totalKg.setScale(KG_SCALE, RoundingMode.HALF_UP),
                cycles.cycles(),
                cycles.perCycleM3(),
                totalKg.divide(BigDecimal.valueOf(cycles.cycles()), KG_SCALE, RoundingMode.HALF_UP),
                List.copyOf(setpoints));
    }
}
