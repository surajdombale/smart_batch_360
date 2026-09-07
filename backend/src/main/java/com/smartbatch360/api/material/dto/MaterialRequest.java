package com.smartbatch360.api.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Just a name. Materials carried a unit and a density until 2026-09-07; every
 * material is now weighed in kilograms, so there is nothing left to choose.
 */
public record MaterialRequest(

        @NotBlank(message = "Material name is required.")
        @Size(max = 100, message = "Material name must be at most 100 characters.")
        String name
) {
}
