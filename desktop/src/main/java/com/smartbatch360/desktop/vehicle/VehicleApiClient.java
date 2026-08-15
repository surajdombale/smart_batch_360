package com.smartbatch360.desktop.vehicle;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VehicleApiClient {

    private static final String BASE_PATH = "/api/v1/vehicles";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<VehicleDto>> list() {
        return apiClient.getList(BASE_PATH, VehicleDto.class);
    }

    public CompletableFuture<VehicleDto> create(VehicleRequestDto request) {
        return apiClient.post(BASE_PATH, request, VehicleDto.class);
    }

    public CompletableFuture<VehicleDto> update(Long id, VehicleRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, VehicleDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
