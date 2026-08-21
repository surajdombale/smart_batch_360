package com.smartbatch360.api.header.dto;

import com.smartbatch360.api.header.Header;
import com.smartbatch360.api.header.HeaderStatus;

import java.time.Instant;

public record HeaderResponse(
        Long id,
        String companyName,
        String plantName,
        String address,
        String phone,
        String email,
        String gstin,
        HeaderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static HeaderResponse from(Header h) {
        return new HeaderResponse(h.getId(), h.getCompanyName(), h.getPlantName(), h.getAddress(),
                h.getPhone(), h.getEmail(), h.getGstin(), h.getStatus(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
