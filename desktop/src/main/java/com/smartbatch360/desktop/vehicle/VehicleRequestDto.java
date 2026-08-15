package com.smartbatch360.desktop.vehicle;

import java.math.BigDecimal;

public record VehicleRequestDto(
        String vehicleNumber,
        Long driverId,
        BigDecimal capacityCubicMeters,
        VehicleStatus status
) {
}
