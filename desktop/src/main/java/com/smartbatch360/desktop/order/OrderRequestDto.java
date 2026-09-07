package com.smartbatch360.desktop.order;

import java.math.BigDecimal;

public record OrderRequestDto(
        Long clientId,
        Long siteId,
        Long recipeId,
        BigDecimal quantityKg
) {
}
