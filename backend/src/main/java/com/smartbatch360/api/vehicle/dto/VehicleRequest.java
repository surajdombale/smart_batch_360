package com.smartbatch360.api.vehicle.dto;

import com.smartbatch360.api.vehicle.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Fields limited to what the Vehicle Management mockup shows: Vehicle Number, Driver, Capacity, Status. */
public record VehicleRequest(

        @NotBlank(message = "Vehicle number is required.")
        @Size(max = 30, message = "Vehicle number must be at most 30 characters.")
        String vehicleNumber,

        /** Optional - a vehicle may have no driver assigned (e.g. status AVAILABLE). */
        Long driverId,

        @NotNull(message = "Capacity is required.")
        @DecimalMin(value = "0.01", message = "Capacity must be greater than zero.")
        @Digits(integer = 4, fraction = 2, message = "Capacity must be a valid number (up to 2 decimal places).")
        BigDecimal capacityCubicMeters,

        @NotNull(message = "Status is required.")
        VehicleStatus status
) {
}
