package com.smartbatch360.desktop.recipe;

import java.math.BigDecimal;
import java.util.List;

public record RecipeRequestDto(
        String name,
        BigDecimal batchSize,
        String description,
        RecipeStatus status,
        List<RecipeMaterialRequestDto> materials
) {
}
