package com.smartbatch360.desktop.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientDto(
        Long id,
        String name,
        String contactPerson,
        String phone,
        String address,
        ClientStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    @Override
    public String toString() {
        return name; // used as the display label in Site's client ComboBox
    }
}
