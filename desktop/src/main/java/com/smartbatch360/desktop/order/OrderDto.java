package com.smartbatch360.desktop.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderDto(
        Long id,
        Long clientId,
        String clientName,
        Long siteId,
        String siteName,
        Long recipeId,
        String recipeName,
        BigDecimal quantityKg,
        BigDecimal producedQuantityKg,
        BigDecimal remainingQuantityKg,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /** ComboBox label wherever an order is picked (e.g. the batch form). */
    @Override
    public String toString() {
        return "#" + id + " - " + recipeName + " (" + quantityKg.toPlainString() + " kg, " + status + ")";
    }
}
