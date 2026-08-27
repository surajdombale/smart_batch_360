package com.smartbatch360.desktop.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartbatch360.desktop.material.MaterialUnit;

import java.math.BigDecimal;

/** materialName/unit are read through from the linked Material by the backend. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeMaterialDto(
        Long id,
        Long materialId,
        String materialName,
        BigDecimal quantity,
        MaterialUnit unit
) {
}
