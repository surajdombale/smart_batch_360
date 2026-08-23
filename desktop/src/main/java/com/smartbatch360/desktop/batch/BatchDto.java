package com.smartbatch360.desktop.batch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchDto(
        Long id,
        String batchNumber,
        Long recipeId,
        String recipeName,
        Long clientId,
        String clientName,
        Long siteId,
        String siteName,
        Long vehicleId,
        String vehicleNumber,
        Long driverId,
        String driverName,
        BigDecimal targetQuantity,
        BigDecimal producedQuantity,
        BigDecimal remainingQuantity,
        Instant cycleDateTime,
        Integer cycleNumber,
        String shift,
        BatchStatus status,
        EquipmentStatus mixerStatus,
        EquipmentStatus conveyorStatus,
        EquipmentStatus waterValveStatus,
        EquipmentStatus cementScrewStatus,
        EquipmentStatus compressorStatus,
        List<BatchMaterialDto> materials,
        Instant createdAt,
        Instant updatedAt
) {
}
