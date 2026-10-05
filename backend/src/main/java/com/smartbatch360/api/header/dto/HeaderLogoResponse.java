package com.smartbatch360.api.header.dto;

/** The stored letterhead image. */
public record HeaderLogoResponse(String contentType, byte[] data) {
}
