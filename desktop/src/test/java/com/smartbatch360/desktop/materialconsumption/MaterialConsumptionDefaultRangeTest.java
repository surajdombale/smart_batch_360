package com.smartbatch360.desktop.materialconsumption;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The screen opens on a bounded range rather than all history. Without one it
 * asked the backend to aggregate every batch ever made on each open, which grew
 * without limit - at two years of production that is hundreds of thousands of
 * material rows for a screen nobody has asked a question of yet.
 */
class MaterialConsumptionDefaultRangeTest {

    @Test
    void theDefaultRangeIsThirtyDaysIncludingToday() {
        LocalDate today = LocalDate.of(2026, 9, 30);

        LocalDate from = MaterialConsumptionView.defaultDateFrom(today);

        assertThat(from).isEqualTo(LocalDate.of(2026, 9, 1));
        // Inclusive of both ends, so the span is 30 days rather than 31.
        assertThat(ChronoUnit.DAYS.between(from, today) + 1).isEqualTo(MaterialConsumptionView.DEFAULT_DAYS);
    }

    /** Crossing a month or a year boundary is just date arithmetic - no special case. */
    @Test
    void itCrossesMonthAndYearBoundariesCorrectly() {
        assertThat(MaterialConsumptionView.defaultDateFrom(LocalDate.of(2027, 1, 5)))
                .isEqualTo(LocalDate.of(2026, 12, 7));
        assertThat(MaterialConsumptionView.defaultDateFrom(LocalDate.of(2028, 3, 1)))
                .isEqualTo(LocalDate.of(2028, 2, 1));   // 2028 is a leap year
    }
}
