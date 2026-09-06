package com.smartbatch360.desktop.vehicle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleDto(
        Long id,
        String vehicleNumber,
        Long driverId,
        String driverName,
        BigDecimal capacityCubicMeters,
        VehicleStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    @Override
    public String toString() {
        return vehicleNumber; // display label in the batch form's vehicle ComboBox
    }
}
