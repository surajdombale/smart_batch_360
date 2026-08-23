package com.smartbatch360.api.client.dto;

import com.smartbatch360.api.client.Client;
import com.smartbatch360.api.client.ClientStatus;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String name,
        String contactPerson,
        String phone,
        String address,
        ClientStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getId(), client.getName(), client.getContactPerson(), client.getPhone(),
                client.getAddress(), client.getStatus(), client.getCreatedAt(), client.getUpdatedAt());
    }
}
