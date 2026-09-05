package com.smartbatch360.desktop.order;

import com.smartbatch360.desktop.api.ApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OrderApiClient {

    private static final String BASE_PATH = "/api/v1/orders";

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<OrderDto>> list() {
        return apiClient.getList(BASE_PATH, OrderDto.class);
    }

    public CompletableFuture<OrderDto> create(OrderRequestDto request) {
        return apiClient.post(BASE_PATH, request, OrderDto.class);
    }

    public CompletableFuture<OrderDto> start(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/start", OrderDto.class);
    }

    public CompletableFuture<OrderDto> fulfil(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/fulfil", OrderDto.class);
    }

    public CompletableFuture<OrderDto> cancel(Long id) {
        return apiClient.postAction(BASE_PATH + "/" + id + "/cancel", OrderDto.class);
    }

    public CompletableFuture<Void> delete(Long id) {
        return apiClient.delete(BASE_PATH + "/" + id);
    }
}
