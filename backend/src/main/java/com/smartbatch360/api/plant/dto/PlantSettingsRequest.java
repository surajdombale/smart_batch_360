package com.smartbatch360.api.plant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Settings > Plant Details. All optional: a plant fills these in when it knows
 * them, and production says which one is missing when it needs it.
 */
public record PlantSettingsRequest(

        @Size(max = 150, message = "Supervisor name must be at most 150 characters.")
        String supervisorName,

        @DecimalMin(value = "0.1", message = "Mixer capacity must be at least 0.1 m3.")
        @DecimalMax(value = "10.0", message = "Mixer capacity must be at most 10 m3.")
        @Digits(integer = 2, fraction = 2, message = "Mixer capacity may have at most 2 decimal places.")
        BigDecimal mixerCapacityM3,

        /** Hourly output, in cubic metres. */
        @DecimalMin(value = "0.01", message = "Plant capacity must be greater than zero.")
        @Digits(integer = 4, fraction = 2, message = "Plant capacity may have at most 2 decimal places.")
        BigDecimal plantCapacityM3PerHour
) {
}
