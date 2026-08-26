package com.smartbatch360.api.materialconsumption;

import java.time.LocalDate;

/**
 * Material Consumption filters. Every field is optional (an absent filter
 * simply isn't applied) except groupBy, which defaults to DAY - same
 * "absent filter" convention as BatchSearchCriteria.
 */
public record MaterialConsumptionSearchCriteria(
        String materialName,
        LocalDate dateFrom,
        LocalDate dateTo,
        MaterialConsumptionGroupBy groupBy
) {
}
