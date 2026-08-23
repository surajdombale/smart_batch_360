package com.smartbatch360.api.batch.dto;

import java.time.LocalDate;

/**
 * Batch Reports filters (docs/02_UI_REFERENCE.md's Batch Reports reference):
 * batch number range, date range, and exact Client/Site/Vehicle/Driver/Recipe
 * matches. Every field is optional - an absent filter simply isn't applied.
 */
public record BatchSearchCriteria(
        String batchNumberFrom,
        String batchNumberTo,
        LocalDate dateFrom,
        LocalDate dateTo,
        Long clientId,
        Long siteId,
        Long vehicleId,
        Long driverId,
        Long recipeId
) {
}
