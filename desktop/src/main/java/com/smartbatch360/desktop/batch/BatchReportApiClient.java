package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.api.ApiClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BatchReportApiClient {

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<BatchPageDto> search(BatchReportFilter filter, int page, int size) {
        return apiClient.get(buildPath(filter, page, size), BatchPageDto.class);
    }

    private String buildPath(BatchReportFilter f, int page, int size) {
        List<String> params = new ArrayList<>();
        addIfPresent(params, "batchNumberFrom", f.batchNumberFrom());
        addIfPresent(params, "batchNumberTo", f.batchNumberTo());
        addIfPresent(params, "dateFrom", f.dateFrom() != null ? f.dateFrom().toString() : null);
        addIfPresent(params, "dateTo", f.dateTo() != null ? f.dateTo().toString() : null);
        addIfPresent(params, "clientId", f.clientId() != null ? String.valueOf(f.clientId()) : null);
        addIfPresent(params, "siteId", f.siteId() != null ? String.valueOf(f.siteId()) : null);
        addIfPresent(params, "vehicleId", f.vehicleId() != null ? String.valueOf(f.vehicleId()) : null);
        addIfPresent(params, "driverId", f.driverId() != null ? String.valueOf(f.driverId()) : null);
        addIfPresent(params, "recipeId", f.recipeId() != null ? String.valueOf(f.recipeId()) : null);
        params.add("page=" + page);
        params.add("size=" + size);
        return "/api/v1/batches/search?" + String.join("&", params);
    }

    private void addIfPresent(List<String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }
}
