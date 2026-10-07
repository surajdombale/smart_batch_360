package com.smartbatch360.api.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One cycle as the PLC reports it: which batch, which cycle, when it ran, and
 * what every material weighed. One message per cycle carrying all materials,
 * which is how the plant produces them.
 *
 * Sending the same cycle twice corrects it rather than adding a second copy, so
 * a PLC that retries after a failed send does not double-count the batch.
 */
public record BatchCycleRequest(

        @NotBlank(message = "Batch number is required.")
        @Size(max = 30, message = "Batch number must be at most 30 characters.")
        String batchNumber,

        @NotNull(message = "Cycle number is required.")
        @Min(value = 1, message = "Cycle number must be 1 or more.")
        Integer cycleNumber,

        @NotNull(message = "Cycle time is required.")
        Instant cycleTime,

        /** Optional, and only meaningful when the load runs with moisture correction. */
        @DecimalMin(value = "0.0", message = "Moisture content cannot be negative.")
        @Digits(integer = 4, fraction = 2,
                message = "Moisture content must be at most 9999.99, with at most 2 decimal places.")
        BigDecimal moistureContent,

        @Digits(integer = 6, fraction = 2,
                message = "Corrected water must be at most 999999.99, with at most 2 decimal places.")
        BigDecimal correctedWater,

        @Digits(integer = 6, fraction = 2,
                message = "Corrected aggregate must be at most 999999.99, with at most 2 decimal places.")
        BigDecimal correctedAggregate,

        @NotEmpty(message = "A cycle must report at least one material.")
        @Size(max = 20, message = "A cycle may report at most 20 materials.")
        @Valid
        List<MaterialAchieved> materials
) {

    /** A cycle that reports weights and nothing else. */
    public BatchCycleRequest(String batchNumber, Integer cycleNumber, Instant cycleTime,
                             List<MaterialAchieved> materials) {
        this(batchNumber, cycleNumber, cycleTime, null, null, null, materials);
    }

    public record MaterialAchieved(

            @NotBlank(message = "Material name is required.")
            @Size(max = 100, message = "Material name must be at most 100 characters.")
            String materialName,

            /**
             * What the cycle was aiming at. Optional: a PLC that does not send
             * them gets the batch's own planned per-cycle figure.
             */
            @DecimalMin(value = "0.0", message = "Target cannot be negative.")
            @Digits(integer = 6, fraction = 2,
                    message = "Target must be at most 999999.99, with at most 2 decimal places.")
            BigDecimal target,

            @DecimalMin(value = "0.0", message = "Setpoint cannot be negative.")
            @Digits(integer = 6, fraction = 2,
                    message = "Setpoint must be at most 999999.99, with at most 2 decimal places.")
            BigDecimal setpoint,

            @NotNull(message = "Achieved quantity is required.")
            @DecimalMin(value = "0.0", message = "Achieved quantity cannot be negative.")
            @Digits(integer = 6, fraction = 2,
                    message = "Achieved quantity must be at most 999999.99, with at most 2 decimal places.")
            BigDecimal achieved,

            @DecimalMin(value = "0.0", message = "Moisture content cannot be negative.")
            @Digits(integer = 4, fraction = 2,
                    message = "Moisture content must be at most 9999.99, with at most 2 decimal places.")
            BigDecimal moistureContent,

            @DecimalMin(value = "0.0", message = "Absorption cannot be negative.")
            @Digits(integer = 4, fraction = 2,
                    message = "Absorption must be at most 9999.99, with at most 2 decimal places.")
            BigDecimal absorption
    ) {

        /** A material that reports its weight and nothing else. */
        public MaterialAchieved(String materialName, BigDecimal achieved) {
            this(materialName, null, null, achieved, null, null);
        }
    }
}
