package com.smartbatch360.api.driver.dto;

import com.smartbatch360.api.driver.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Fields limited to what the Driver Management mockup shows: Driver Name, Phone, License No., Status. */
public record DriverRequest(

        @NotBlank(message = "Driver name is required.")
        @Size(max = 150, message = "Driver name must be at most 150 characters.")
        String name,

        @NotBlank(message = "Phone is required.")
        @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid phone number.")
        String phone,

        @NotBlank(message = "License number is required.")
        @Size(max = 50, message = "License number must be at most 50 characters.")
        String licenseNo,

        @NotNull(message = "Status is required.")
        DriverStatus status
) {
}
