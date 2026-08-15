package com.smartbatch360.api.vehicle.dto;

import com.smartbatch360.api.vehicle.Vehicle;
import com.smartbatch360.api.vehicle.VehicleStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record VehicleResponse(
        Long id,
        String vehicleNumber,
        Long driverId,
        String driverName,
        BigDecimal capacityCubicMeters,
        VehicleStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(
                v.getId(),
                v.getVehicleNumber(),
                v.getDriver() != null ? v.getDriver().getId() : null,
                v.getDriver() != null ? v.getDriver().getName() : null,
                v.getCapacityCubicMeters(),
                v.getStatus(),
                v.getCreatedAt(),
                v.getUpdatedAt());
    }
}
