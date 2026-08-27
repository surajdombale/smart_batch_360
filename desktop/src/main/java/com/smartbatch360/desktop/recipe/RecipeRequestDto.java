package com.smartbatch360.desktop.recipe;

import java.util.List;

/** No batch quantity: it is derived server-side from the materials. */
public record RecipeRequestDto(
        String name,
        String description,
        RecipeStatus status,
        List<RecipeMaterialRequestDto> materials
) {
}
