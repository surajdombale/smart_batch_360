package com.smartbatch360.api.materialconsumption;

import com.smartbatch360.api.material.MaterialUnit;

import java.math.BigDecimal;

/**
 * How much of one material an order consumes: the recipe's per-batch quantity
 * scaled up to the ordered volume. Reported in the material's own unit (KG /
 * LITRE), which is what a plant operator actually needs.
 */
public record OrderMaterialConsumptionResponse(
        Long materialId,
        String materialName,
        MaterialUnit unit,
        BigDecimal quantity
) {
}
