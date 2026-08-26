package com.smartbatch360.desktop.materialconsumption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MaterialConsumptionDto(
        String materialName,
        String unit,
        String period,
        BigDecimal totalTarget,
        BigDecimal totalAchieved,
        BigDecimal variance,
        long batchCount
) {
}
