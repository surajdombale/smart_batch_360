package com.smartbatch360.desktop.batch;

import java.time.LocalDate;

/** Batch Reports filters - every field optional (null = not applied). */
public record BatchReportFilter(
        String batchNumberFrom,
        String batchNumberTo,
        LocalDate dateFrom,
        LocalDate dateTo,
        Long clientId,
        Long siteId,
        Long vehicleId,
        Long driverId,
        Long recipeId
) {
}
