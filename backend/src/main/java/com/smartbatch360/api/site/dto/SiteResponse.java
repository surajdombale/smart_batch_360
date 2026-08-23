package com.smartbatch360.api.site.dto;

import com.smartbatch360.api.site.Site;
import com.smartbatch360.api.site.SiteStatus;

import java.time.Instant;

public record SiteResponse(
        Long id,
        String name,
        Long clientId,
        String clientName,
        String location,
        SiteStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SiteResponse from(Site s) {
        return new SiteResponse(
                s.getId(),
                s.getName(),
                s.getClient().getId(),
                s.getClient().getName(),
                s.getLocation(),
                s.getStatus(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
