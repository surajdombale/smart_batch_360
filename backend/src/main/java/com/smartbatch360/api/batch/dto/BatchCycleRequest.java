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

        @NotEmpty(message = "A cycle must report at least one material.")
        @Size(max = 20, message = "A cycle may report at most 20 materials.")
        @Valid
        List<MaterialAchieved> materials
) {

    public record MaterialAchieved(

            @NotBlank(message = "Material name is required.")
            @Size(max = 100, message = "Material name must be at most 100 characters.")
            String materialName,

            @NotNull(message = "Achieved quantity is required.")
            @DecimalMin(value = "0.0", message = "Achieved quantity cannot be negative.")
            @Digits(integer = 6, fraction = 2,
                    message = "Achieved quantity must be at most 999999.99, with at most 2 decimal places.")
            BigDecimal achieved
    ) {
    }
}
