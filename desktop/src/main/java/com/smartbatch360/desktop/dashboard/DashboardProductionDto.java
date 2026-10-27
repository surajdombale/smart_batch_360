package com.smartbatch360.desktop.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartbatch360.desktop.batch.BatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardProductionDto(
        DashboardPeriod period,
        LocalDate firstDay,
        LocalDate lastDay,
        Totals current,
        Totals previous,
        List<TrendPoint> trend,
        List<RecentBatch> recentBatches
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Totals(long batches, BigDecimal producedM3, long activeVehicles) {
    }

    /** start is a plant-local hour or day, already in the plant's zone. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrendPoint(LocalDateTime start, long batches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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
