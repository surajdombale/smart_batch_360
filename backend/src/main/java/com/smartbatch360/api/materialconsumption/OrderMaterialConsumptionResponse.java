package com.smartbatch360.api.materialconsumption;

import java.math.BigDecimal;

/**
 * How much of one material an order consumes: the recipe's per-batch quantity
 * scaled up to the ordered quantity. In kilograms, like everything else.
 */
public record OrderMaterialConsumptionResponse(
        Long materialId,
        String materialName,
        BigDecimal quantityKg
) {
}
