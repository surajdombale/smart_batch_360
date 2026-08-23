package com.smartbatch360.api.batch.dto;

import com.smartbatch360.api.batch.Batch;
import org.springframework.data.domain.Page;

import java.util.List;

public record BatchPageResponse(
        List<BatchResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static BatchPageResponse from(Page<Batch> page) {
        return new BatchPageResponse(
                page.getContent().stream().map(BatchResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
