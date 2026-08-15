package com.smartbatch360.desktop.customer;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CustomerApiClient {

    private static final String BASE_PATH = "/api/v1/customers";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<CustomerDto>> list() {
        return apiClient.getList(BASE_PATH, CustomerDto.class);
    }

    public CompletableFuture<CustomerDto> create(CustomerRequestDto request) {
        return apiClient.post(BASE_PATH, request, CustomerDto.class);
    }

    public CompletableFuture<CustomerDto> update(Long id, CustomerRequestDto request) {
        return apiClient.put(BASE_PATH + "/" + id, request, CustomerDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
