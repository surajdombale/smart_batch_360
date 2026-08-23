package com.smartbatch360.desktop.recipe;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RecipeApiClient {

    private static final String BASE_PATH = "/api/v1/recipes";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<RecipeDto>> list() {
        return apiClient.getList(BASE_PATH, RecipeDto.class);
    }

    public CompletableFuture<RecipeDto> create(RecipeRequestDto request) {
        return apiClient.post(BASE_PATH, request, RecipeDto.class);
    }

    public CompletableFuture<RecipeDto> update(Long id, RecipeRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, RecipeDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
