package com.smartbatch360.desktop.client;

public record ClientRequestDto(
        String name,
        String contactPerson,
        String phone,
        String address,
        ClientStatus status
) {
}
