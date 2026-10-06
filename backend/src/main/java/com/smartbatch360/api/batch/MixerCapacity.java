package com.smartbatch360.api.batch;

import com.smartbatch360.api.common.InvalidRequestException;

import java.math.BigDecimal;

/**
 * How much the plant's mixer holds, in cubic metres.
 *
 * Production is planned in m3 so an operator can see the size of a load and
 * match it to a vehicle, whose capacity is already recorded in m3. The mixer
 * cannot hold a whole load at once, so a batch is run as repeated cycles - this
 * is the size of one of them.
 *
 * Set on Company Details, falling back to smartbatch360.plant.mixer-capacity-m3
 * - see PlantSettings, which decides which applies. It belongs to the plant, so
 * it is not guessed here; unset means production planning has nothing to divide
 * by and the caller is told where to set it.
 */
public class MixerCapacity {

    /** The range the plant's own specification gives for a mixer. */
    static final BigDecimal MINIMUM_M3 = new BigDecimal("0.1");
    static final BigDecimal MAXIMUM_M3 = new BigDecimal("10");

    private final BigDecimal capacityM3;

    public MixerCapacity(String configured) {
        if (configured == null || configured.isBlank()) {
            this.capacityM3 = null;
            return;
        }
        BigDecimal value = new BigDecimal(configured.trim());
        if (value.compareTo(MINIMUM_M3) < 0 || value.compareTo(MAXIMUM_M3) > 0) {
            throw new IllegalArgumentException("Mixer capacity must be between " + MINIMUM_M3.toPlainString()
                    + " and " + MAXIMUM_M3.toPlainString() + " m3, but is " + value.toPlainString() + ".");
        }
        this.capacityM3 = value;
    }

    /** For tests and any caller that needs a specific capacity rather than the configured one. */
    public static MixerCapacity of(String capacityM3) {
        return new MixerCapacity(capacityM3);
    }

    public static MixerCapacity notConfigured() {
        return new MixerCapacity("");
    }

    public boolean isConfigured() {
        return capacityM3 != null;
    }

    public BigDecimal capacityM3() {
        if (capacityM3 == null) {
            throw new InvalidRequestException("The plant's mixer capacity has not been set, so a batch cannot "
                    + "be split into cycles. Set it on Company Details.");
        }
        return capacityM3;
    }
}
