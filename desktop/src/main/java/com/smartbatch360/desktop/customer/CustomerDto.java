package com.smartbatch360.desktop.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerDto(
        Long id,
        String name,
        String contactPerson,
        String phone,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    @Override
    public String toString() {
        return name; // used as the display label in Site's customer ComboBox
    }
}
