package com.smartbatch360.desktop.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchPageDto(
        List<BatchDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
