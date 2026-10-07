package com.smartbatch360.desktop.production;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** What the backend works a load out to, for the Production screen to display. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductionPlanDto(
        Long orderId,
        String clientName,
        String siteName,
        Long siteId,
        String recipeName,
        Long recipeId,
        BigDecimal orderQuantityKg,
        BigDecimal orderRemainingKg,
        BigDecimal batchSizeM3,
        BigDecimal mixerCapacityM3,
        int cycles,
        BigDecimal perCycleM3,
        BigDecimal perCycleKg,
        BigDecimal totalKg,
        List<MaterialSetpointDto> materials
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MaterialSetpointDto(String materialName, BigDecimal recipeQuantityKg,
                                       BigDecimal perCycleKg, BigDecimal totalKg) {
    }
}
