package com.smartbatch360.api.header.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A letterhead image, sent base64-encoded in JSON rather than as a multipart
 * upload - the desktop client already speaks JSON, and a logo is small enough
 * that the encoding overhead costs nothing worth a second transport.
 */
public record HeaderLogoRequest(

        @NotBlank(message = "Image type is required.")
        String contentType,

        @NotNull(message = "Image data is required.")
        byte[] data
) {
}
