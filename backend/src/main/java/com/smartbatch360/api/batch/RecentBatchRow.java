package com.smartbatch360.api.batch;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One line of the dashboard's recent batches, reduced to what it shows. Read as
 * a projection with the recipe and vehicle joined in, so the list is one query
 * rather than one per row.
 */
public record RecentBatchRow(
        Long id,
        String batchNumber,
        String recipeName,
        String vehicleNumber,
        BigDecimal producedKg,
        Instant cycleDateTime,
        BatchStatus status
) {
}
