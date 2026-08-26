package com.smartbatch360.api.materialconsumption;

import java.math.BigDecimal;

/**
 * One aggregated row: a material's total target vs actual (achieved) for one
 * period bucket. variance = totalAchieved - totalTarget, so positive means
 * more material was consumed than planned (wastage/over-consumption) and
 * negative means less (under-consumption) - docs/02_UI_REFERENCE.md's
 * "material target vs actual" / "variance/wastage".
 */
public record MaterialConsumptionResponse(
        String materialName,
        String unit,
        String period,
        BigDecimal totalTarget,
        BigDecimal totalAchieved,
        BigDecimal variance,
        long batchCount
) {
}
