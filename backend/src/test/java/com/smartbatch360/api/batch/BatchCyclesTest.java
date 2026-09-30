package com.smartbatch360.api.batch;

import com.smartbatch360.api.common.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Splitting a load into mixer cycles, from the plant's own formula: cycles is
 * batch size over mixer capacity, and per-cycle quantity is batch size over
 * cycles.
 */
class BatchCyclesTest {

    private static final MixerCapacity ONE_M3 = MixerCapacity.of("1");

    @Test
    void aLoadThatDividesEvenlyRunsFullCycles() {
        BatchCycles plan = BatchCycles.plan(new BigDecimal("6"), ONE_M3);

        assertThat(plan.cycles()).isEqualTo(6);
        assertThat(plan.perCycleM3()).isEqualByComparingTo("1");
    }

    /**
     * The case the formula alone does not settle: 6.5 m3 is 6.5 mixer loads,
     * and half a cycle cannot be scheduled. Rounded up and spread evenly, so
     * every cycle carries the same mix and none exceeds the mixer.
     */
    @Test
    void aLoadThatDoesNotDivideEvenlyIsSpreadOverOneMoreCycle() {
        BatchCycles plan = BatchCycles.plan(new BigDecimal("6.5"), ONE_M3);

        assertThat(plan.cycles()).isEqualTo(7);
        assertThat(plan.perCycleM3()).isEqualByComparingTo("0.9286");
        assertThat(plan.perCycleM3()).isLessThanOrEqualTo(new BigDecimal("1"));
    }

    @Test
    void aLoadSmallerThanTheMixerIsASingleCycle() {
        BatchCycles plan = BatchCycles.plan(new BigDecimal("0.4"), ONE_M3);

        assertThat(plan.cycles()).isEqualTo(1);
        assertThat(plan.perCycleM3()).isEqualByComparingTo("0.4");
    }

    @Test
    void aSmallMixerTakesManyCycles() {
        BatchCycles plan = BatchCycles.plan(new BigDecimal("2"), MixerCapacity.of("0.25"));

        assertThat(plan.cycles()).isEqualTo(8);
        assertThat(plan.perCycleM3()).isEqualByComparingTo("0.25");
    }

    /** The smallest load the plant allows still plans. */
    @Test
    void theSmallestAllowedLoadIsOneCycle() {
        BatchCycles plan = BatchCycles.plan(new BigDecimal("0.1"), ONE_M3);

        assertThat(plan.cycles()).isEqualTo(1);
        assertThat(plan.perCycleM3()).isEqualByComparingTo("0.1");
    }

    @Test
    void aLoadBelowTheMinimumIsRefused() {
        assertThatThrownBy(() -> BatchCycles.plan(new BigDecimal("0.05"), ONE_M3))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at least 0.1 m3");
        assertThatThrownBy(() -> BatchCycles.plan(null, ONE_M3))
                .isInstanceOf(InvalidRequestException.class);
    }

    /** Without a mixer capacity there is nothing to divide by, and the message says so. */
    @Test
    void planningIsRefusedUntilTheMixerCapacityIsSet() {
        assertThatThrownBy(() -> BatchCycles.plan(new BigDecimal("6"), MixerCapacity.notConfigured()))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("mixer capacity has not been set");
    }

    /**
     * The setpoint step: the recipe gives what the whole load needs, and each
     * cycle takes its share. Shares must add back up to the load.
     */
    @Test
    void eachCycleTakesItsShareOfAMaterial() {
        BatchCycles plan = BatchCycles.plan(new BigDecimal("6"), ONE_M3);

        BigDecimal perCycle = plan.shareOf(new BigDecimal("1920.00"));

        assertThat(perCycle).isEqualByComparingTo("320");
        assertThat(perCycle.multiply(BigDecimal.valueOf(plan.cycles()))).isEqualByComparingTo("1920.00");
    }

    @Test
    void theMixerCapacityMustBeWithinThePlantsRange() {
        assertThatThrownBy(() -> MixerCapacity.of("0.05"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0.1 and 10");
        assertThatThrownBy(() -> MixerCapacity.of("12"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(MixerCapacity.of("0.1").capacityM3()).isEqualByComparingTo("0.1");
        assertThat(MixerCapacity.of("10").capacityM3()).isEqualByComparingTo("10");
    }
}
