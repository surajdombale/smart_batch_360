package com.smartbatch360.desktop.driver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DriverDto(
        Long id,
        String name,
        String phone,
        String licenseNo,
        DriverStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    @Override
    public String toString() {
        return name; // used as the display label in Vehicle's driver ComboBox
    }
}
