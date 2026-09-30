package com.smartbatch360.api.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Report dates mean days at the plant, not UTC days.
 *
 * Cycle times are stored as true UTC instants, so for a plant five and a half
 * hours ahead of UTC every batch made between midnight and 05:30 falls on the
 * previous UTC day. Converting with UTC filed those night-shift batches under
 * the wrong day and hid them from a filter on the day they were made.
 */
class ReportingZoneTest {

    private static final ZoneId PLANT = ZoneId.of("Asia/Kolkata");

    @Test
    void aDayStartsAtMidnightAtThePlant() {
        Instant start = ReportingZone.of(PLANT).startOfDay(LocalDate.of(2026, 9, 26));

        // 2026-09-26 00:00 at the plant is 2026-09-25 18:30 UTC.
        assertThat(start).isEqualTo(Instant.parse("2026-09-25T18:30:00Z"));
    }

    @Test
    void aDayEndsWhereTheNextOneStarts() {
        ReportingZone zone = ReportingZone.of(PLANT);

        assertThat(zone.startOfNextDay(LocalDate.of(2026, 9, 26)))
                .isEqualTo(zone.startOfDay(LocalDate.of(2026, 9, 27)))
                .isEqualTo(Instant.parse("2026-09-26T18:30:00Z"));
    }

    /** The case that was wrong: a batch made at 01:30 on a night shift. */
    @Test
    void aNightShiftBatchBelongsToTheDayThePlantWasOn() {
        Instant nightShift = Instant.parse("2026-09-25T20:00:00Z");   // 01:30 on the 26th at the plant

        assertThat(ReportingZone.of(PLANT).dayOf(nightShift)).isEqualTo(LocalDate.of(2026, 9, 26));
        // What it used to report, for contrast.
        assertThat(ReportingZone.of(ZoneOffset.UTC).dayOf(nightShift)).isEqualTo(LocalDate.of(2026, 9, 25));
    }

    @Test
    void aDaytimeBatchIsUnaffected() {
        Instant afternoon = Instant.parse("2026-09-25T10:00:00Z");    // 15:30 at the plant

        assertThat(ReportingZone.of(PLANT).dayOf(afternoon)).isEqualTo(LocalDate.of(2026, 9, 25));
    }

    @Test
    void anUnsetConfigurationMeansTheZoneTheServerRunsIn() {
        assertThat(new ReportingZone("").zoneId()).isEqualTo(ZoneId.systemDefault());
        assertThat(new ReportingZone(null).zoneId()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void aConfiguredZoneIsUsed() {
        assertThat(new ReportingZone("Asia/Kolkata").zoneId()).isEqualTo(PLANT);
        assertThat(new ReportingZone("  UTC  ").zoneId()).isEqualTo(ZoneId.of("UTC"));
    }
}
