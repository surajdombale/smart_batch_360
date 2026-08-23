package com.smartbatch360.desktop.site;

public record SiteRequestDto(
        String name,
        Long clientId,
        String location,
        SiteStatus status
) {
}
