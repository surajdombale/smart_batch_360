package com.smartbatch360.api.batch.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Starting production, which is the last step on the Production screen.
 *
 * The customer, site and recipe are not sent: they come from the order, and
 * letting the screen override them is how a batch ends up disagreeing with the
 * order it claims to fulfil. The driver comes from the chosen vehicle.
 */
public record StartProductionRequest(

        @NotNull(message = "An order is required.")
        Long orderId,

        @NotNull(message = "A vehicle is required.")
        Long vehicleId,

        @NotNull(message = "Batch size is required.")
        @DecimalMin(value = "0.1", message = "Batch size must be at least 0.1 m3.")
        @DecimalMax(value = "1000.0", message = "Batch size must be at most 1000 m3.")
        @Digits(integer = 4, fraction = 2, message = "Batch size may have at most 2 decimal places.")
        BigDecimal batchSizeM3,

        /** Optional - left blank, the plant's own numbering is used. */
        @Size(max = 30, message = "Batch number must be at most 30 characters.")
        String batchNumber,

        @Size(max = 50, message = "Shift must be at most 50 characters.")
        String shift,

        /** Optional - left out, the load runs without moisture correction. */
        Boolean moistureEnabled,

        /**
         * Optional - left out, the vehicle's own driver is used. Last in the
         * list on purpose: it was added after the shapes below were in use, and
         * putting it in the middle silently changed what those calls meant.
         */
        Long driverId
) {

    public StartProductionRequest(Long orderId, Long vehicleId, BigDecimal batchSizeM3, String batchNumber,
                                  String shift) {
        this(orderId, vehicleId, batchSizeM3, batchNumber, shift, null, null);
    }

    public StartProductionRequest(Long orderId, Long vehicleId, BigDecimal batchSizeM3, String batchNumber,
                                  String shift, Boolean moistureEnabled) {
        this(orderId, vehicleId, batchSizeM3, batchNumber, shift, moistureEnabled, null);
    }
}
