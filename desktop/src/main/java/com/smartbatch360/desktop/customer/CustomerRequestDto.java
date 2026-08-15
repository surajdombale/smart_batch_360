package com.smartbatch360.desktop.customer;

public record CustomerRequestDto(
        String name,
        String contactPerson,
        String phone,
        CustomerStatus status
) {
}
