package com.smartbatch360.api.recipe.dto;

import com.smartbatch360.api.recipe.RecipeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Fields per the Recipe Management mockup: Recipe Name, Batch Size, Description, Material Proportion list, Status. */
public record RecipeRequest(

        @NotBlank(message = "Recipe name is required.")
        @Size(max = 150, message = "Recipe name must be at most 150 characters.")
        String name,

        @NotNull(message = "Batch size is required.")
        @DecimalMin(value = "0.01", message = "Batch size must be greater than zero.")
        BigDecimal batchSize,

        @Size(max = 255, message = "Description must be at most 255 characters.")
        String description,

        @NotNull(message = "Status is required.")
        RecipeStatus status,

        @NotEmpty(message = "At least one material is required.")
        @Valid
        List<RecipeMaterialRequest> materials
) {
}
