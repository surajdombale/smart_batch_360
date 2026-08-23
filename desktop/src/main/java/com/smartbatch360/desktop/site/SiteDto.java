package com.smartbatch360.desktop.site;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SiteDto(
        Long id,
        String name,
        Long clientId,
        String clientName,
        String location,
        SiteStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
