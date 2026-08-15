package com.smartbatch360.desktop.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardSummaryDto(
        long totalCustomers,
        long totalSites,
        long totalVehicles,
        long totalDrivers,
        String backendStatus,
        String databaseStatus,
        String apiStatus
) {
}
