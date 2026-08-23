package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BatchApiClient {

    private static final String BASE_PATH = "/api/v1/batches";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<BatchDto>> list() {
        return apiClient.getList(BASE_PATH, BatchDto.class);
    }

    public CompletableFuture<BatchDto> create(BatchRequestDto request) {
        return apiClient.post(BASE_PATH, request, BatchDto.class);
    }

    public CompletableFuture<BatchDto> update(Long id, BatchRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, BatchDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }

    public CompletableFuture<BatchDto> start(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/start", BatchDto.class);
    }

    public CompletableFuture<BatchDto> pause(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/pause", BatchDto.class);
    }

    public CompletableFuture<BatchDto> resume(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/resume", BatchDto.class);
    }

    public CompletableFuture<BatchDto> stop(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/stop", BatchDto.class);
    }

    public CompletableFuture<BatchDto> emergencyStop(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/emergency-stop", BatchDto.class);
    }

    public CompletableFuture<BatchDto> complete(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/complete", BatchDto.class);
    }
}
