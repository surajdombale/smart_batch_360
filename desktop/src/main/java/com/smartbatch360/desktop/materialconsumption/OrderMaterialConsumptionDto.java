package com.smartbatch360.desktop.materialconsumption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderMaterialConsumptionDto(
        Long materialId,
        String materialName,
        BigDecimal quantityKg
) {
}
