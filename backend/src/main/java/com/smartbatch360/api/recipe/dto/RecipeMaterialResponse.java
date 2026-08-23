package com.smartbatch360.api.recipe.dto;

import com.smartbatch360.api.recipe.RecipeMaterial;

import java.math.BigDecimal;

public record RecipeMaterialResponse(
        Long id,
        String materialName,
        BigDecimal quantity,
        String unit
) {
    public static RecipeMaterialResponse from(RecipeMaterial m) {
        return new RecipeMaterialResponse(m.getId(), m.getMaterialName(), m.getQuantity(), m.getUnit());
    }
}
