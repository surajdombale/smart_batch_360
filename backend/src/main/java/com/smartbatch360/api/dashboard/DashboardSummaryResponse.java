package com.smartbatch360.api.dashboard;

/**
 * Phase 1 dashboard data only. KPIs that depend on Production/Recipe/BatchData
 * (Total Batches, Today's Volume, Revenue, Production Trend, Recent Batches,
 * Active Recipe, Last Batch, Average Batch Time, PLC status) are intentionally
 * absent: those data sources do not exist in this phase
 * (docs/01_REQUIREMENTS.md, docs/02_UI_REFERENCE.md).
 */
public record DashboardSummaryResponse(
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
