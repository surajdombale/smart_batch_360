package com.smartbatch360.api.recipe.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecipeMaterialRequest(

        @NotBlank(message = "Material name is required.")
        @Size(max = 100, message = "Material name must be at most 100 characters.")
        String materialName,

        @NotNull(message = "Quantity is required.")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
        BigDecimal quantity,

        @NotBlank(message = "Unit is required.")
        @Size(max = 20, message = "Unit must be at most 20 characters.")
        String unit
) {
}
