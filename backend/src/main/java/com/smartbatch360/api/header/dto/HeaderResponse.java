package com.smartbatch360.api.header.dto;

import com.smartbatch360.api.header.Header;
import com.smartbatch360.api.header.HeaderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The logo is reported as a flag rather than as bytes: the list screen shows
 * every company, and sending a letterhead image with each row would make a
 * cheap screen expensive. The image itself has its own endpoint.
 */
public record HeaderResponse(
        Long id,
        String companyName,
        String plantName,
        String address,
        String city,
        String pinCode,
        String phone,
        String email,
        String gstin,
        String supervisorName,
        Integer mixTimeSeconds,
        Integer dischargeTimeSeconds,
        BigDecimal mixerCapacityM3,
        boolean hasLogo,
        HeaderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static HeaderResponse from(Header h) {
        return new HeaderResponse(h.getId(), h.getCompanyName(), h.getPlantName(), h.getAddress(),
                h.getCity(), h.getPinCode(), h.getPhone(), h.getEmail(), h.getGstin(),
                h.getSupervisorName(), h.getMixTimeSeconds(), h.getDischargeTimeSeconds(),
                h.getMixerCapacityM3(),
                h.getLogo() != null && h.getLogo().length > 0,
                h.getStatus(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
