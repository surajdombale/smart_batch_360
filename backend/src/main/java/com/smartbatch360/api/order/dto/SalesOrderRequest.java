package com.smartbatch360.api.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Create Order: Customer (the existing Client entity), Site, Recipe, quantity
 * in m3. Status is not accepted - a new order is always UNFULFILLED.
 */
public record SalesOrderRequest(

        @NotNull(message = "Customer is required.")
        Long clientId,

        @NotNull(message = "Site is required.")
        Long siteId,

        @NotNull(message = "Recipe is required.")
        Long recipeId,

        @NotNull(message = "Order quantity is required.")
        @DecimalMin(value = "0.01", message = "Order quantity must be greater than zero.")
        BigDecimal quantityM3
) {
}
