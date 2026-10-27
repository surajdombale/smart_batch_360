package com.smartbatch360.api.dashboard;

import com.smartbatch360.api.batch.BatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What the plant produced over a dashboard period, set against the period
 * before it.
 *
 * Volumes are in m3 because that is how a load is sized and matched to a
 * vehicle. They are the produced kilograms through ConcreteDensity - the one
 * number joining the two units - so an in-progress batch counts what it has
 * made so far, and a total is always the sum of the batches beneath it.
 *
 * Revenue is absent on purpose: nothing in the schema carries a price.
 */
public record DashboardProductionResponse(
        DashboardPeriod period,
        LocalDate firstDay,
        LocalDate lastDay,
        Totals current,
        /**
         * The same span immediately before. For a period running up to now, it
         * stops at the same time of day - today so far against yesterday until
         * this time - or every morning would read as a collapse.
         */
        Totals previous,
        /** Plant-local hours for a single day, plant-local days otherwise. */
        List<TrendPoint> trend,
        List<RecentBatch> recentBatches
) {

    /** activeVehicles counts the vehicles that carried at least one batch. */
    public record Totals(long batches, BigDecimal producedM3, long activeVehicles) {
    }

    /** start is plant-local: the first moment of the hour or day it counts. */
    public record TrendPoint(LocalDateTime start, long batches) {
    }

    public record RecentBatch(
            Long id,
            String batchNumber,
            String recipeName,
            String vehicleNumber,
            BigDecimal producedM3,
            Instant cycleDateTime,
            BatchStatus status
    ) {
    }
}
