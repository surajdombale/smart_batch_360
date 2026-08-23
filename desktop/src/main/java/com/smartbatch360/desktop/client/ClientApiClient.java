package com.smartbatch360.desktop.client;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClientApiClient {

    private static final String BASE_PATH = "/api/v1/clients";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<ClientDto>> list() {
        return apiClient.getList(BASE_PATH, ClientDto.class);
    }

    public CompletableFuture<ClientDto> create(ClientRequestDto request) {
        return apiClient.post(BASE_PATH, request, ClientDto.class);
    }

    public CompletableFuture<ClientDto> update(Long id, ClientRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, ClientDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
