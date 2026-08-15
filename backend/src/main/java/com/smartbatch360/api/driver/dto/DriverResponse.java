package com.smartbatch360.api.driver.dto;

import com.smartbatch360.api.driver.Driver;
import com.smartbatch360.api.driver.DriverStatus;

import java.time.Instant;

public record DriverResponse(
        Long id,
        String name,
        String phone,
        String licenseNo,
        DriverStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static DriverResponse from(Driver d) {
        return new DriverResponse(d.getId(), d.getName(), d.getPhone(), d.getLicenseNo(),
                d.getStatus(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
