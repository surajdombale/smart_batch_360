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
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SalesOrderResponse from(SalesOrder o) {
        return new SalesOrderResponse(
                o.getId(),
                o.getClient().getId(),
                o.getClient().getName(),
                o.getSite().getId(),
                o.getSite().getName(),
                o.getRecipe().getId(),
                o.getRecipe().getName(),
                o.getQuantityM3(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getUpdatedAt());
    }
}
