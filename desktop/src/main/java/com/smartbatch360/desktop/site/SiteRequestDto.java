package com.smartbatch360.desktop.site;

public record SiteRequestDto(
        String name,
        Long customerId,
        String location,
        SiteStatus status
) {
}
