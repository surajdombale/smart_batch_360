package com.smartbatch360.desktop.materialconsumption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartbatch360.desktop.material.MaterialUnit;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderMaterialConsumptionDto(
        Long materialId,
        String materialName,
        MaterialUnit unit,
        BigDecimal quantity
) {
}
