package com.smartbatch360.desktop.header;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HeaderDto(
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
}
