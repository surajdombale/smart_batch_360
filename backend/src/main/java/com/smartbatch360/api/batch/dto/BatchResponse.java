package com.smartbatch360.api.batch.dto;

import com.smartbatch360.api.batch.Batch;
import com.smartbatch360.api.batch.BatchStatus;
import com.smartbatch360.api.batch.EquipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BatchResponse(
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
        List<BatchMaterialResponse> materials,
        Instant createdAt,
        Instant updatedAt
) {
    public static BatchResponse from(Batch b) {
        BigDecimal remaining = b.getTargetQuantity().subtract(b.getProducedQuantity());
        return new BatchResponse(
                b.getId(),
                b.getBatchNumber(),
                b.getRecipe().getId(),
                b.getRecipe().getName(),
                b.getClient().getId(),
                b.getClient().getName(),
                b.getSite().getId(),
                b.getSite().getName(),
                b.getVehicle().getId(),
                b.getVehicle().getVehicleNumber(),
                b.getDriver().getId(),
                b.getDriver().getName(),
                b.getTargetQuantity(),
                b.getProducedQuantity(),
                remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining,
                b.getCycleDateTime(),
                b.getCycleNumber(),
                b.getShift(),
                b.getStatus(),
                b.getMixerStatus(),
                b.getConveyorStatus(),
                b.getWaterValveStatus(),
                b.getCementScrewStatus(),
                b.getCompressorStatus(),
                b.getMaterials().stream().map(BatchMaterialResponse::from).toList(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }
}
