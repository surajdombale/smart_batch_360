package com.smartbatch360.api.materialconsumption;

import java.math.BigDecimal;
import java.util.List;

/** An order's projected material consumption, derived from Order -> Recipe -> Recipe Materials -> Material. */
public record OrderConsumptionResponse(
        Long orderId,
        String clientName,
        String siteName,
        Long recipeId,
        String recipeName,
        BigDecimal orderQuantityM3,
        BigDecimal recipeBatchQuantityM3,
        List<OrderMaterialConsumptionResponse> materials
) {
}
