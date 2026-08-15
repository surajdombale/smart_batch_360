package com.smartbatch360.api.customer.dto;

import com.smartbatch360.api.customer.Customer;
import com.smartbatch360.api.customer.CustomerStatus;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String name,
        String contactPerson,
        String phone,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getContactPerson(), c.getPhone(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
