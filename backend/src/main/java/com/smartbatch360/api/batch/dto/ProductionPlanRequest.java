package com.smartbatch360.api.batch.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * What the Production screen asks for while the operator is still typing: how
 * big a load, against which order. Everything else on the screen - the cycles,
 * the per-cycle quantity, the setpoints - is calculated from these two.
 */
public record ProductionPlanRequest(

        @NotNull(message = "An order is required.")
        Long orderId,

        @NotNull(message = "Batch size is required.")
        @DecimalMin(value = "0.1", message = "Batch size must be at least 0.1 m3.")
        @DecimalMax(value = "1000.0", message = "Batch size must be at most 1000 m3.")
        @Digits(integer = 4, fraction = 2, message = "Batch size may have at most 2 decimal places.")
        BigDecimal batchSizeM3
) {
}
