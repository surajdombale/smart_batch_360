package com.smartbatch360.api.recipe.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** One "Add Material" row: which Material, and how much of it in that material's own unit. */
public record RecipeMaterialRequest(

        @NotNull(message = "Material is required.")
        Long materialId,

        @NotNull(message = "Quantity is required.")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
        BigDecimal quantity
) {
}
