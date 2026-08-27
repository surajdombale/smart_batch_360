package com.smartbatch360.api.material.dto;

import com.smartbatch360.api.material.MaterialUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Material Name + Unit, plus a density that is required only for weight-based
 * (KG) units - validated in MaterialService, since whether it is required
 * depends on the chosen unit.
 */
public record MaterialRequest(

        @NotBlank(message = "Material name is required.")
        @Size(max = 100, message = "Material name must be at most 100 characters.")
        String name,

        @NotNull(message = "Unit is required.")
        MaterialUnit unit,

        @DecimalMin(value = "0.001", message = "Density must be greater than zero.")
        BigDecimal densityKgPerM3
) {
}
