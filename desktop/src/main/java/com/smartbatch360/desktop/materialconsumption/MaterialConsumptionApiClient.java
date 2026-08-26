package com.smartbatch360.desktop.materialconsumption;

import com.smartbatch360.desktop.api.ApiClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MaterialConsumptionApiClient {

    private final ApiClient apiClient = new ApiClient();

    public CompletableFuture<List<MaterialConsumptionDto>> search(String materialName, LocalDate dateFrom,
                                                                    LocalDate dateTo, MaterialConsumptionGroupBy groupBy) {
        List<String> params = new ArrayList<>();
        addIfPresent(params, "materialName", materialName);
        addIfPresent(params, "dateFrom", dateFrom != null ? dateFrom.toString() : null);
        addIfPresent(params, "dateTo", dateTo != null ? dateTo.toString() : null);
        addIfPresent(params, "groupBy", groupBy != null ? groupBy.name() : null);
        String path = "/api/v1/material-consumption/search"
                + (params.isEmpty() ? "" : "?" + String.join("&", params));
        return apiClient.getList(path, MaterialConsumptionDto.class);
    }

    private void addIfPresent(List<String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }
}
