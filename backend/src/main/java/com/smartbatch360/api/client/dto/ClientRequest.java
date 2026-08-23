package com.smartbatch360.api.client.dto;

import com.smartbatch360.api.client.ClientStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Fields limited to what the (originally "Customer Management") mockup
 * shows - Client Name, Contact Person, Phone, Status - plus Address, added
 * at the user's explicit request (2026-08-23). Address is optional since it
 * has no mockup backing and isn't always available.
 */
public record ClientRequest(

        @NotBlank(message = "Client name is required.")
        @Size(max = 150, message = "Client name must be at most 150 characters.")
        String name,

        @NotBlank(message = "Contact person is required.")
        @Size(max = 150, message = "Contact person must be at most 150 characters.")
        String contactPerson,

        @NotBlank(message = "Phone is required.")
        @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid phone number.")
        String phone,

        @Size(max = 255, message = "Address must be at most 255 characters.")
        String address,

        @NotNull(message = "Status is required.")
        ClientStatus status
) {
}
