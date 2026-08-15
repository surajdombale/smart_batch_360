package com.smartbatch360.desktop.driver;

public record DriverRequestDto(
        String name,
        String phone,
        String licenseNo,
        DriverStatus status
) {
}
