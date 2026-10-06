package com.smartbatch360.desktop.production;

import com.smartbatch360.desktop.api.ApiClient;
import com.smartbatch360.desktop.batch.BatchDto;

import java.util.concurrent.CompletableFuture;

public class ProductionApiClient {

    private static final String BASE_PATH = "/api/v1/batches";

    private final ApiClient apiClient = new ApiClient();

    /** Works a load out without changing anything - called as the operator types. */
    public CompletableFuture<ProductionPlanDto> plan(ProductionPlanRequestDto request) {
        return apiClient.post(BASE_PATH + "/plan", request, ProductionPlanDto.class);
    }

    public CompletableFuture<BatchDto> start(StartProductionRequestDto request) {
        return apiClient.post(BASE_PATH + "/start-production", request, BatchDto.class);
    }
}
