package com.smartbatch360.api.batch.dto;

import com.smartbatch360.api.batch.BatchStatus;
import com.smartbatch360.api.batch.EquipmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Fields per the Production mockup's Batch Details/Current Batch panel (docs/02_UI_REFERENCE.md). */
public record BatchRequest(

        @NotBlank(message = "Batch number is required.")
        @Size(max = 30, message = "Batch number must be at most 30 characters.")
        String batchNumber,

        @NotNull(message = "Recipe is required.")
        Long recipeId,

        @NotNull(message = "Client is required.")
        Long clientId,

        @NotNull(message = "Site is required.")
        Long siteId,

        @NotNull(message = "Vehicle is required.")
        Long vehicleId,

        @NotNull(message = "Driver is required.")
        Long driverId,

        @NotNull(message = "Target quantity is required.")
        @DecimalMin(value = "0.01", message = "Target quantity must be greater than zero.")
        BigDecimal targetQuantity,

        @NotNull(message = "Produced quantity is required.")
        @DecimalMin(value = "0.00", message = "Produced quantity cannot be negative.")
        BigDecimal producedQuantity,

        Instant cycleDateTime,

        Integer cycleNumber,

        @Size(max = 50, message = "Shift must be at most 50 characters.")
        String shift,

        @NotNull(message = "Status is required.")
        BatchStatus status,

        @NotNull(message = "Mixer status is required.")
        EquipmentStatus mixerStatus,

        @NotNull(message = "Conveyor status is required.")
        EquipmentStatus conveyorStatus,

        @NotNull(message = "Water valve status is required.")
        EquipmentStatus waterValveStatus,

        @NotNull(message = "Cement screw status is required.")
        EquipmentStatus cementScrewStatus,

        @NotNull(message = "Compressor status is required.")
        EquipmentStatus compressorStatus,

        @NotEmpty(message = "At least one material is required.")
        @Valid
        List<BatchMaterialRequest> materials
) {
}
