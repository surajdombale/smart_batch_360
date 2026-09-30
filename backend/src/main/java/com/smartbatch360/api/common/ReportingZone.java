package com.smartbatch360.api.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The zone the plant's days are measured in, and the only place a date becomes
 * an instant or an instant becomes a day.
 *
 * Cycle times are stored as true UTC instants. A report date is not a UTC day
 * though - it is a day at the plant. Converting with UTC put a batch made at
 * 01:30 on a night shift into the previous day's bucket and hid it from a filter
 * on the day it was actually made, which for a plant five and a half hours from
 * UTC is every batch between midnight and 05:30.
 *
 * Configured with smartbatch360.reporting.zone; the default is the zone the
 * server runs in, which for a single-plant install is the plant's own.
 */
@Component
public class ReportingZone {

    private final ZoneId zoneId;

    public ReportingZone(@Value("${smartbatch360.reporting.zone:}") String configured) {
        this.zoneId = configured == null || configured.isBlank()
                ? ZoneId.systemDefault()
                : ZoneId.of(configured.trim());
    }

    /** For tests and any caller that needs a specific zone rather than the configured one. */
    public static ReportingZone of(ZoneId zoneId) {
        return new ReportingZone(zoneId.getId());
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    /** First instant of the given plant-local day. */
    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(zoneId).toInstant();
    }

    /**
     * First instant after the given plant-local day, for an exclusive upper
     * bound - so a filter to a single day covers that whole day and nothing of
     * the next.
     */
    public Instant startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(zoneId).toInstant();
    }

    /** The plant-local day an instant falls on. */
    public LocalDate dayOf(Instant instant) {
        return instant.atZone(zoneId).toLocalDate();
    }
}
