package com.smartbatch360.api.batch.dto;

import com.smartbatch360.api.batch.BatchPlan;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the Production screen shows back: the order's own details so the
 * operator can see what they are producing against, and the calculated figures
 * the plant's formula gives.
 */
public record ProductionPlanResponse(
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
        List<MaterialSetpointResponse> materials
) {

    public record MaterialSetpointResponse(String materialName, BigDecimal perCycleKg, BigDecimal totalKg) {
    }

    public static ProductionPlanResponse of(Long orderId, String clientName, String siteName, Long siteId,
                                             String recipeName, Long recipeId, BigDecimal orderQuantityKg,
                                             BigDecimal orderRemainingKg, BigDecimal mixerCapacityM3,
                                             BatchPlan plan) {
        return new ProductionPlanResponse(
                orderId, clientName, siteName, siteId, recipeName, recipeId,
                orderQuantityKg, orderRemainingKg,
                plan.batchSizeM3(), mixerCapacityM3, plan.cycles(), plan.perCycleM3(), plan.perCycleKg(),
                plan.totalKg(),
                plan.materials().stream()
                        .map(m -> new MaterialSetpointResponse(m.materialName(), m.perCycleKg(), m.totalKg()))
                        .toList());
    }
}
