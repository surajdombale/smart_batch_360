package com.smartbatch360.desktop.header;

public record HeaderRequestDto(
        String companyName,
        String plantName,
        String address,
        String city,
        String pinCode,
        String phone,
        String email,
        String gstin,
        HeaderStatus status
) {
}
