package com.smartbatch360.desktop.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeMaterialDto(
        Long id,
        String materialName,
        BigDecimal quantity,
        String unit
) {
}
