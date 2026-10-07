package com.smartbatch360.api.plant.dto;

import com.smartbatch360.api.plant.PlantSettingsEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record PlantSettingsResponse(
        String supervisorName,
        BigDecimal mixerCapacityM3,
        BigDecimal plantCapacityM3PerHour,
        Instant updatedAt
) {
    public static PlantSettingsResponse from(PlantSettingsEntity settings) {
        return new PlantSettingsResponse(settings.getSupervisorName(), settings.getMixerCapacityM3(),
                settings.getPlantCapacityM3PerHour(), settings.getUpdatedAt());
    }
}
