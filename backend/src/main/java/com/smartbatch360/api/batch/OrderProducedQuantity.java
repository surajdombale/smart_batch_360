package com.smartbatch360.api.batch;

import java.math.BigDecimal;

/**
 * How much has been produced against one order, as returned by the grouped
 * query behind the order list. Lets the list sum every order's batches in a
 * single round trip instead of one query per order.
 */
public record OrderProducedQuantity(Long orderId, BigDecimal producedQuantity) {
}
