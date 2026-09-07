package com.smartbatch360.desktop.material;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MaterialDto(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    /** ComboBox rendering - shown wherever a material is picked. */
    @Override
    public String toString() {
        return name;
    }
}
