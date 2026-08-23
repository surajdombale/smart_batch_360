package com.smartbatch360.api.batch.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BatchMaterialRequest(

        @NotBlank(message = "Material name is required.")
        @Size(max = 100, message = "Material name must be at most 100 characters.")
        String materialName,

        @NotNull(message = "Target is required.")
        @DecimalMin(value = "0.00", message = "Target cannot be negative.")
        BigDecimal target,

        @NotNull(message = "Setpoint is required.")
        @DecimalMin(value = "0.00", message = "Setpoint cannot be negative.")
        BigDecimal setpoint,

        @NotNull(message = "Achieved is required.")
        @DecimalMin(value = "0.00", message = "Achieved cannot be negative.")
        BigDecimal achieved,

        @NotBlank(message = "Unit is required.")
        @Size(max = 20, message = "Unit must be at most 20 characters.")
        String unit
) {
}
