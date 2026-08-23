package com.smartbatch360.desktop.recipe;

import java.math.BigDecimal;

public record RecipeMaterialRequestDto(
        String materialName,
        BigDecimal quantity,
        String unit
) {
}
