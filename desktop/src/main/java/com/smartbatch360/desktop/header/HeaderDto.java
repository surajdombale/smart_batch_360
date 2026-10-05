package com.smartbatch360.desktop.header;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HeaderDto(
        Long id,
        String companyName,
        String plantName,
        String address,
        String city,
        String pinCode,
        String phone,
        String email,
        String gstin,
        String supervisorName,
        Integer mixTimeSeconds,
        Integer dischargeTimeSeconds,
        boolean hasLogo,
        HeaderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
