package com.smartbatch360.desktop.materialconsumption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConsumptionDto(
        Long orderId,
        String clientName,
        String siteName,
        Long recipeId,
        String recipeName,
        BigDecimal orderQuantityM3,
        BigDecimal recipeBatchQuantityM3,
        List<OrderMaterialConsumptionDto> materials
) {
}
