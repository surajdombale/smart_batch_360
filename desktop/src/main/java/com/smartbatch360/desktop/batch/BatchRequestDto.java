package com.smartbatch360.desktop.batch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BatchRequestDto(
        String batchNumber,
        Long recipeId,
        Long clientId,
        Long siteId,
        Long vehicleId,
        Long driverId,
        BigDecimal targetQuantity,
        BigDecimal producedQuantity,
        Instant cycleDateTime,
        Integer cycleNumber,
        String shift,
        BatchStatus status,
        EquipmentStatus mixerStatus,
        EquipmentStatus conveyorStatus,
        EquipmentStatus waterValveStatus,
        EquipmentStatus cementScrewStatus,
        EquipmentStatus compressorStatus,
        List<BatchMaterialRequestDto> materials
) {
}
