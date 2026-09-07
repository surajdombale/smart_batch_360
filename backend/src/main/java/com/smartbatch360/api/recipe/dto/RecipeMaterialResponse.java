package com.smartbatch360.api.recipe.dto;

import com.smartbatch360.api.recipe.RecipeMaterial;

import java.math.BigDecimal;

/**
 * materialName is read through to the linked Material rather than stored on the
 * line itself. The unit that used to sit here is gone - every quantity is
 * kilograms.
 */
public record RecipeMaterialResponse(
        Long id,
        Long materialId,
        String materialName,
        BigDecimal quantity
) {
    public static RecipeMaterialResponse from(RecipeMaterial m) {
        return new RecipeMaterialResponse(
                m.getId(),
                m.getMaterial().getId(),
                m.getMaterial().getName(),
                m.getQuantity());
    }
}
