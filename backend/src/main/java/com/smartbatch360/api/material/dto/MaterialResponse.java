package com.smartbatch360.api.material.dto;

import com.smartbatch360.api.material.Material;

import java.time.Instant;

public record MaterialResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    public static MaterialResponse from(Material m) {
        return new MaterialResponse(m.getId(), m.getName(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
