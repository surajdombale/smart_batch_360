package com.smartbatch360.api.site.dto;

import com.smartbatch360.api.site.SiteStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Fields limited to what the Site Management mockup shows: Site Name, Client (originally "Customer"), Location, Status. */
public record SiteRequest(

        @NotBlank(message = "Site name is required.")
        @Size(max = 150, message = "Site name must be at most 150 characters.")
        String name,

        @NotNull(message = "Client is required.")
        Long clientId,

        @NotBlank(message = "Location is required.")
        @Size(max = 150, message = "Location must be at most 150 characters.")
        String location,

        @NotNull(message = "Status is required.")
        SiteStatus status
) {
}
