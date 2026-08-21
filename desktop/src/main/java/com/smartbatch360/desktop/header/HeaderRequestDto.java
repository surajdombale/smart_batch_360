package com.smartbatch360.desktop.header;

public record HeaderRequestDto(
        String companyName,
        String plantName,
        String address,
        String phone,
        String email,
        String gstin,
        HeaderStatus status
) {
}
