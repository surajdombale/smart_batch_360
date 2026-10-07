package com.smartbatch360.desktop.plant;

import java.math.BigDecimal;

public record PlantSettingsRequestDto(
        String supervisorName,
        BigDecimal mixerCapacityM3,
        BigDecimal plantCapacityM3PerHour
) {
}
