package com.smartbatch360.desktop.plant;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.concurrent.CompletableFuture;

public class PlantSettingsApiClient {

    private static final String BASE_PATH = "/api/v1/plant-settings";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<PlantSettingsDto> get() {
        return apiClient.get(BASE_PATH, PlantSettingsDto.class);
    }

    public CompletableFuture<PlantSettingsDto> save(PlantSettingsRequestDto request) {
        return apiClient.put(BASE_PATH, request, PlantSettingsDto.class);
    }
}
