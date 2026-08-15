package com.smartbatch360.desktop.driver;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DriverApiClient {

    private static final String BASE_PATH = "/api/v1/drivers";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<DriverDto>> list() {
        return apiClient.getList(BASE_PATH, DriverDto.class);
    }

    public CompletableFuture<DriverDto> create(DriverRequestDto request) {
        return apiClient.post(BASE_PATH, request, DriverDto.class);
    }

    public CompletableFuture<DriverDto> update(Long id, DriverRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, DriverDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
