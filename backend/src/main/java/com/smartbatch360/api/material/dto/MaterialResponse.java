package com.smartbatch360.api.material.dto;

import com.smartbatch360.api.material.Material;
import com.smartbatch360.api.material.MaterialUnit;

import java.math.BigDecimal;
import java.time.Instant;

public record MaterialResponse(
        Long id,
        String name,
        MaterialUnit unit,
        BigDecimal densityKgPerM3,
        Instant createdAt,
        Instant updatedAt
) {
    public static MaterialResponse from(Material m) {
        return new MaterialResponse(
                m.getId(),
                m.getName(),
                m.getUnit(),
                m.getDensityKgPerM3(),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }
}
