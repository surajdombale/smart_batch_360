package com.smartbatch360.desktop.header;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HeaderApiClient {

    private static final String BASE_PATH = "/api/v1/headers";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<HeaderDto>> list() {
        return apiClient.getList(BASE_PATH, HeaderDto.class);
    }

    public CompletableFuture<HeaderDto> create(HeaderRequestDto request) {
        return apiClient.post(BASE_PATH, request, HeaderDto.class);
    }

    public CompletableFuture<HeaderDto> update(Long id, HeaderRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, HeaderDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
