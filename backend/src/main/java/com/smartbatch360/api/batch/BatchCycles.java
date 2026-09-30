package com.smartbatch360.api.batch;

import com.smartbatch360.api.common.InvalidRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How a load is split into mixer cycles.
 *
 * From the plant's own description: a batch size is entered in m3, the number
 * of cycles is the batch size over the mixer capacity, and the quantity per
 * cycle is the batch size over that number of cycles.
 *
 * Cycles are whole - half a mixer run is not a thing to schedule - so a load
 * that does not divide evenly is rounded UP and then spread evenly across the
 * cycles it needs. 6.5 m3 through a 1 m3 mixer is 7 cycles of 0.929 m3 rather
 * than 6 of 1 m3 and a seventh of 0.5: an even split keeps every cycle the same
 * mix and avoids a final part-load the mixer may weigh badly.
 */
public record BatchCycles(int cycles, BigDecimal perCycleM3) {

    /** Matches the scale batch quantities are stored at. */
    private static final int SCALE = 4;

    public static BatchCycles plan(BigDecimal batchSizeM3, MixerCapacity mixerCapacity) {
        if (batchSizeM3 == null || batchSizeM3.compareTo(MixerCapacity.MINIMUM_M3) < 0) {
            throw new InvalidRequestException("Batch size must be at least "
                    + MixerCapacity.MINIMUM_M3.toPlainString() + " m3.");
        }
        BigDecimal capacity = mixerCapacity.capacityM3();

        int cycles = batchSizeM3.divide(capacity, 0, RoundingMode.CEILING).intValueExact();
        BigDecimal perCycle = batchSizeM3.divide(BigDecimal.valueOf(cycles), SCALE, RoundingMode.HALF_UP);
        return new BatchCycles(cycles, perCycle);
    }

    /**
     * What one cycle takes of a material, given how much of it the whole load
     * needs. This is the "adjust setpoint according to per cycle capacity" step:
     * the recipe gives the load, the mixer runs a fraction of it at a time.
     */
    public BigDecimal shareOf(BigDecimal quantityForWholeBatch) {
        return quantityForWholeBatch.divide(BigDecimal.valueOf(cycles), SCALE, RoundingMode.HALF_UP);
    }
}
