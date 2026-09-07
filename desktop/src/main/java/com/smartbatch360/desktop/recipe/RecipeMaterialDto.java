package com.smartbatch360.desktop.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** materialName is read through from the linked Material by the backend. Quantity is kilograms. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeMaterialDto(
        Long id,
        Long materialId,
        String materialName,
        BigDecimal quantity
) {
}
