package com.smartbatch360.desktop.recipe;

import java.math.BigDecimal;

public record RecipeMaterialRequestDto(
        Long materialId,
        BigDecimal quantity
) {
}
