package com.smartbatch360.desktop.material;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MaterialApiClient {

    private static final String BASE_PATH = "/api/v1/materials";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<MaterialDto>> list() {
        return apiClient.getList(BASE_PATH, MaterialDto.class);
    }

    public CompletableFuture<MaterialDto> create(MaterialRequestDto request) {
        return apiClient.post(BASE_PATH, request, MaterialDto.class);
    }

    public CompletableFuture<MaterialDto> update(Long id, MaterialRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, MaterialDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
