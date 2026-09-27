package com.smartbatch360.api.batch;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One batch material line, reduced to the five fields Material Consumption
 * aggregates. The screen sums target and achieved per material and period, so
 * loading whole BatchMaterial and Batch entities for it built an object graph
 * of everything the plant has ever produced - 180,000 entities at 30,000
 * batches - to read five values from each.
 */
public record MaterialConsumptionRow(
        String materialName,
        String unit,
        BigDecimal target,
        BigDecimal achieved,
        Instant cycleDateTime
) {
}
