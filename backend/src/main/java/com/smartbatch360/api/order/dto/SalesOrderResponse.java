package com.smartbatch360.api.order.dto;

import com.smartbatch360.api.order.OrderStatus;
import com.smartbatch360.api.order.SalesOrder;

import java.math.BigDecimal;
import java.time.Instant;

public record SalesOrderResponse(
        Long id,
        Long clientId,
        String clientName,
        Long siteId,
        String siteName,
        Long recipeId,
        String recipeName,
        BigDecimal quantityM3,
        /** Total m3 produced against this order so far, summed from its batches. */
        BigDecimal producedQuantityM3,
        /** Ordered minus produced, never negative (over-production isn't a debt). */
        BigDecimal remainingQuantityM3,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    /** For callers that haven't computed fulfilment (e.g. straight after create). */
    public static SalesOrderResponse from(SalesOrder o) {
        return from(o, BigDecimal.ZERO);
    }

    public static SalesOrderResponse from(SalesOrder o, BigDecimal producedM3) {
        return new SalesOrderResponse(
                o.getId(),
                o.getClient().getId(),
                o.getClient().getName(),
                o.getSite().getId(),
                o.getSite().getName(),
                o.getRecipe().getId(),
                o.getRecipe().getName(),
                o.getQuantityM3(),
                producedM3,
                remaining(o.getQuantityM3(), producedM3),
                o.getStatus(),
                o.getCreatedAt(),
                o.getUpdatedAt());
    }

    private static BigDecimal remaining(BigDecimal ordered, BigDecimal produced) {
        BigDecimal left = ordered.subtract(produced);
        return left.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : left;
    }
}
