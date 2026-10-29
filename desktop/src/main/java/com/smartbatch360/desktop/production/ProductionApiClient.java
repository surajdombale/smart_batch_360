package com.smartbatch360.desktop.production;

import com.smartbatch360.desktop.api.ApiClient;
import com.smartbatch360.desktop.batch.BatchCycleDto;
import com.smartbatch360.desktop.batch.BatchDto;

import java.util.List;
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

    /** The batch the plant is running - empty when there is none. */
    public CompletableFuture<List<BatchDto>> running() {
        return apiClient.getList(BASE_PATH + "/running", BatchDto.class);
    }

    public CompletableFuture<BatchDto> batch(Long id) {
        return apiClient.get(BASE_PATH + "/" + id, BatchDto.class);
    }

    /** Every cycle the batch has recorded so far, in order. */
    public CompletableFuture<List<BatchCycleDto>> cycles(Long batchId) {
        return apiClient.getList("/api/v1/batch-cycles/batch/" + batchId, BatchCycleDto.class);
    }

    /**
     * Records a cycle through the endpoint the PLC uses, so a cycle typed in
     * here is held to exactly the same rules as one the plant reports.
     */
    public CompletableFuture<BatchCycleDto> recordCycle(BatchCycleRequestDto request) {
        return apiClient.post("/api/v1/batch-cycles", request, BatchCycleDto.class);
    }

    public CompletableFuture<BatchDto> stop(Long batchId) {
        return apiClient.postAction(BASE_PATH + "/" + batchId + "/stop", BatchDto.class);
    }
}
