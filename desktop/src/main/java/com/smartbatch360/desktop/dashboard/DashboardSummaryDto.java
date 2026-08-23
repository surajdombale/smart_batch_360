package com.smartbatch360.desktop.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardSummaryDto(
        long totalClients,
        long totalSites,
        long totalVehicles,
        long totalDrivers,
        long totalHeaders,
        long totalRecipes,
        long totalBatches,
        String backendStatus,
        String databaseStatus,
        String apiStatus
) {
}
