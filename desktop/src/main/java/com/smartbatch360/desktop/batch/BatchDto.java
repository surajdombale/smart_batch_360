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
        Long orderId,
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
        List<BatchMaterialDto> materials,
        Instant createdAt,
        Instant updatedAt,
        /** The load as it was sized, in m3. Null on a batch entered by hand. */
        BigDecimal batchSizeM3,
        BigDecimal perCycleM3
) {

    /** A batch with no recorded size - one entered by hand rather than planned. */
    public BatchDto(Long id, String batchNumber, Long recipeId, String recipeName, Long orderId, Long clientId,
                    String clientName, Long siteId, String siteName, Long vehicleId, String vehicleNumber,
                    Long driverId, String driverName, BigDecimal targetQuantity, BigDecimal producedQuantity,
                    BigDecimal remainingQuantity, Instant cycleDateTime, Integer cycleNumber, String shift,
                    BatchStatus status, List<BatchMaterialDto> materials, Instant createdAt, Instant updatedAt) {
        this(id, batchNumber, recipeId, recipeName, orderId, clientId, clientName, siteId, siteName, vehicleId,
                vehicleNumber, driverId, driverName, targetQuantity, producedQuantity, remainingQuantity,
                cycleDateTime, cycleNumber, shift, status, materials, createdAt, updatedAt, null, null);
    }
}
