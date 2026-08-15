package com.smartbatch360.desktop.site;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SiteApiClient {

    private static final String BASE_PATH = "/api/v1/sites";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<SiteDto>> list() {
        return apiClient.getList(BASE_PATH, SiteDto.class);
    }

    public CompletableFuture<SiteDto> create(SiteRequestDto request) {
        return apiClient.post(BASE_PATH, request, SiteDto.class);
    }

    public CompletableFuture<SiteDto> update(Long id, SiteRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, SiteDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
