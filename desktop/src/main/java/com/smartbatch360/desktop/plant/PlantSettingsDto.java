package com.smartbatch360.desktop.plant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlantSettingsDto(
        String supervisorName,
        BigDecimal mixerCapacityM3,
        BigDecimal plantCapacityM3PerHour,
        Instant updatedAt
) {
}
