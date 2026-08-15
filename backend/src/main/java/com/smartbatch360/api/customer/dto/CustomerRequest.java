package com.smartbatch360.api.customer.dto;

import com.smartbatch360.api.customer.CustomerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Fields limited to what the Customer Management mockup shows: Customer Name, Contact Person, Phone, Status. */
public record CustomerRequest(

        @NotBlank(message = "Customer name is required.")
        @Size(max = 150, message = "Customer name must be at most 150 characters.")
        String name,

        @NotBlank(message = "Contact person is required.")
        @Size(max = 150, message = "Contact person must be at most 150 characters.")
        String contactPerson,

        @NotBlank(message = "Phone is required.")
        @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid phone number.")
        String phone,

        @NotNull(message = "Status is required.")
        CustomerStatus status
) {
}
