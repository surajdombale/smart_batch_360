package com.smartbatch360.api.batch;

import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.material.Material;
import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeMaterial;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Planning a load: cubic metres in, per-cycle setpoints out.
 *
 * The conversion between m3 and kilograms is the concrete density the user gave
 * (2400 kg/m3), and the split between materials is the recipe's own proportions.
 * Nothing here assumes a per-material density, which the project forbids.
 */
class BatchPlannerTest {

    private static final MixerCapacity ONE_M3_MIXER = MixerCapacity.of("1");

    private final BatchPlanner planner = new BatchPlanner(ConcreteDensity.standard());

    /** A recipe totalling 1200 kg, half cement and half sand, for easy arithmetic. */
    private Recipe recipe() {
        Recipe recipe = new Recipe();
        recipe.setName("M25");
        recipe.getMaterials().add(line(recipe, "Cement", "600"));
        recipe.getMaterials().add(line(recipe, "Sand", "600"));
        recipe.recalculateTotalBatchQuantity();
        return recipe;
    }

    private RecipeMaterial line(Recipe recipe, String materialName, String quantity) {
        Material material = new Material();
        material.setName(materialName);
        RecipeMaterial line = new RecipeMaterial();
        line.setRecipe(recipe);
        line.setMaterial(material);
        line.setQuantity(new BigDecimal(quantity));
        return line;
    }

    @Test
    void aLoadsWeightComesFromItsVolume() {
        BatchPlan plan = planner.plan(recipe(), new BigDecimal("2"), ONE_M3_MIXER);

        // 2 m3 of concrete at 2400 kg/m3.
        assertThat(plan.totalKg()).isEqualByComparingTo("4800.00");
        assertThat(plan.cycles()).isEqualTo(2);
        assertThat(plan.perCycleM3()).isEqualByComparingTo("1");
        assertThat(plan.perCycleKg()).isEqualByComparingTo("2400.00");
    }

    /**
     * The recipe does the splitting. 4800 kg of a mix that is half cement is
     * 2400 kg of cement, which over 2 cycles is 1200 kg a cycle.
     */
    @Test
    void eachMaterialIsScaledByTheRecipesOwnProportions() {
        BatchPlan plan = planner.plan(recipe(), new BigDecimal("2"), ONE_M3_MIXER);

        assertThat(plan.materials()).extracting(BatchPlan.MaterialSetpoint::materialName)
                .containsExactly("Cement", "Sand");
        assertThat(plan.materials().get(0).perCycleKg()).isEqualByComparingTo("1200.00");
        assertThat(plan.materials().get(0).totalKg()).isEqualByComparingTo("2400.00");
    }

    /**
     * The batch report adds the cycle rows up and compares them with "Set
     * total", so the per-cycle figure multiplied by the cycles must BE the
     * total. Deriving the total independently would leave the report
     * disagreeing with itself by a rounding error.
     */
    @Test
    void theSetpointTotalIsExactlyThePerCycleFigureMultipliedOut() {
        BatchPlan plan = planner.plan(recipe(), new BigDecimal("3.5"), ONE_M3_MIXER);   // 4 cycles, does not divide evenly

        assertThat(plan.cycles()).isEqualTo(4);
        for (BatchPlan.MaterialSetpoint setpoint : plan.materials()) {
            assertThat(setpoint.totalKg())
                    .isEqualByComparingTo(setpoint.perCycleKg().multiply(BigDecimal.valueOf(plan.cycles())));
        }
    }

    /** The smallest load the plant allows still plans a sensible mix. */
    @Test
    void asmallLoadStillSplitsCorrectly() {
        BatchPlan plan = planner.plan(recipe(), new BigDecimal("0.5"), ONE_M3_MIXER);

        assertThat(plan.cycles()).isEqualTo(1);
        assertThat(plan.totalKg()).isEqualByComparingTo("1200.00");
        assertThat(plan.materials().get(0).perCycleKg()).isEqualByComparingTo("600.00");
    }

    /** A different plant density changes the weights and nothing else. */
    @Test
    void theDensityIsConfigurable() {
        BatchPlanner lighter = new BatchPlanner(ConcreteDensity.of("2300"));

        BatchPlan plan = lighter.plan(recipe(), new BigDecimal("1"), ONE_M3_MIXER);

        assertThat(plan.totalKg()).isEqualByComparingTo("2300.00");
        assertThat(plan.materials().get(0).perCycleKg()).isEqualByComparingTo("1150.00");
    }

    @Test
    void anEmptyRecipeCannotBePlanned() {
        Recipe empty = new Recipe();
        empty.setName("Nothing");
        empty.recalculateTotalBatchQuantity();

        assertThatThrownBy(() -> planner.plan(empty, new BigDecimal("1"), ONE_M3_MIXER))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("has no materials");
    }

    @Test
    void planningNeedsAMixerCapacity() {
        BatchPlanner unconfigured = new BatchPlanner(ConcreteDensity.standard());

        assertThatThrownBy(() -> unconfigured.plan(recipe(), new BigDecimal("1"), MixerCapacity.notConfigured()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("mixer capacity has not been set");
    }
}
