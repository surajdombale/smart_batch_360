package com.smartbatch360.api.batch;

import java.math.BigDecimal;

/**
 * How much the plant made in a stretch of time: the batches, the kilograms they
 * produced, and how many vehicles carried them. Aggregated in one query, so the
 * dashboard costs the same at a hundred batches as at thirty thousand.
 */
public record BatchTotals(long batches, BigDecimal producedKg, long vehicles) {
}
