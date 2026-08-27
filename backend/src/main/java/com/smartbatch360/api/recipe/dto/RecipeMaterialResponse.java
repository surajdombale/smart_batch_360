package com.smartbatch360.api.recipe.dto;

import com.smartbatch360.api.material.MaterialUnit;
import com.smartbatch360.api.recipe.RecipeMaterial;

import java.math.BigDecimal;

/**
 * materialName/unit are read through to the linked Material rather than stored
 * on the line itself. They stay in the response shape so existing consumers
 * (Production's recipe auto-fill) keep working unchanged.
 */
public record RecipeMaterialResponse(
        Long id,
        Long materialId,
        String materialName,
        BigDecimal quantity,
        MaterialUnit unit
) {
    public static RecipeMaterialResponse from(RecipeMaterial m) {
        return new RecipeMaterialResponse(
                m.getId(),
                m.getMaterial().getId(),
                m.getMaterial().getName(),
                m.getQuantity(),
                m.getMaterial().getUnit());
    }
}
