package com.smartbatch360.api.recipe.dto;

import com.smartbatch360.api.recipe.RecipeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Recipe Name + the material list. The total batch quantity is intentionally
 * NOT accepted from the client - it is derived from the materials server-side
 * (Recipe#recalculateTotalBatchQuantity).
 */
public record RecipeRequest(

        @NotBlank(message = "Recipe name is required.")
        @Size(max = 150, message = "Recipe name must be at most 150 characters.")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters.")
        String description,

        @NotNull(message = "Status is required.")
        RecipeStatus status,

        @NotEmpty(message = "At least one material is required.")
        @Valid
        List<RecipeMaterialRequest> materials
) {
}
