package com.smartbatch360.desktop.header;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A letterhead image, base64-encoded by Jackson on the way across. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HeaderLogoDto(String contentType, byte[] data) {
}
