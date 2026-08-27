package com.smartbatch360.api.recipe.dto;

import com.smartbatch360.api.recipe.Recipe;
import com.smartbatch360.api.recipe.RecipeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecipeResponse(
        Long id,
        String name,
        BigDecimal totalBatchQuantityM3,
        String description,
        RecipeStatus status,
        List<RecipeMaterialResponse> materials,
        Instant createdAt,
        Instant updatedAt
) {
    public static RecipeResponse from(Recipe r) {
        return new RecipeResponse(
                r.getId(),
                r.getName(),
                r.getTotalBatchQuantityM3(),
                r.getDescription(),
                r.getStatus(),
                r.getMaterials().stream().map(RecipeMaterialResponse::from).toList(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
