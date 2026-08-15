package com.smartbatch360.desktop.dashboard;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.concurrent.CompletableFuture;

public class DashboardApiClient {

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<DashboardSummaryDto> getSummary() {
        return apiClient.get("/api/v1/dashboard/summary", DashboardSummaryDto.class);
    }
}
