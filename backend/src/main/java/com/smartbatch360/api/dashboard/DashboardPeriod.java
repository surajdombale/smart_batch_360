package com.smartbatch360.api.dashboard;

import java.time.LocalDate;

/**
 * The run of plant days the dashboard covers. Each is compared with the same
 * number of days immediately before it.
 */
public enum DashboardPeriod {
    TODAY(1, 0),
    YESTERDAY(1, 1),
    LAST_7_DAYS(7, 0),
    LAST_30_DAYS(30, 0);

    private final int days;
    private final int daysBeforeToday;

    DashboardPeriod(int days, int daysBeforeToday) {
        this.days = days;
        this.daysBeforeToday = daysBeforeToday;
    }

    public int days() {
        return days;
    }

    public LocalDate firstDay(LocalDate today) {
        return lastDay(today).minusDays(days - 1L);
    }

    public LocalDate lastDay(LocalDate today) {
        return today.minusDays(daysBeforeToday);
    }

    /** Whether the period runs up to now, so its last day is still in progress. */
    public boolean endsToday() {
        return daysBeforeToday == 0;
    }

    /** A single day is charted by the hour; anything longer by the day. */
    public boolean hourly() {
        return days == 1;
    }
}
