package com.smartbatch360.api.dashboard;

/**
 * Record counts and the backend/database status. What the plant produced -
 * batches, volume, vehicles, the trend and the latest batches - comes from
 * /dashboard/production, which takes a period; these figures do not.
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
