package com.smartbatch360.desktop.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeDto(
        Long id,
        String name,
        BigDecimal batchSize,
        String description,
        RecipeStatus status,
        List<RecipeMaterialDto> materials,
        Instant createdAt,
        Instant updatedAt
) {
    @Override
    public String toString() {
        return name; // used as the display label in Production's recipe ComboBox
    }
}
