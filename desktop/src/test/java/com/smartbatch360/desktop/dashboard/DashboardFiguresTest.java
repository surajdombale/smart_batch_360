package com.smartbatch360.desktop.dashboard;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardFiguresTest {

    private static final DecimalFormat COUNT = new DecimalFormat("#,##0");
    private static final DecimalFormat VOLUME = new DecimalFormat("#,##0.00");

    @Test
    void aChangeIsAPercentageOfWhatCameBefore() {
        assertThat(change("24", "20", COUNT, "")).isEqualTo("▲ 20% vs this time yesterday");
        assertThat(change("15", "20", COUNT, "")).isEqualTo("▼ 25% vs this time yesterday");
    }

    @Test
    void aRiseFromNothingIsGivenAsTheAmount() {
        assertThat(change("3", "0", COUNT, "")).isEqualTo("▲ +3 vs this time yesterday");
        assertThat(change("30.02", "0", VOLUME, " m³")).isEqualTo("▲ +30.02 m³ vs this time yesterday");
    }

    @Test
    void noMovementSaysSo() {
        assertThat(change("0", "0", COUNT, "")).isEqualTo("No change vs this time yesterday");
        assertThat(change("4.50", "4.5", VOLUME, " m³")).isEqualTo("No change vs this time yesterday");
    }

    /** Rounding a real change to "0%" would read as no change at all. */
    @Test
    void aSmallChangeIsNotRoundedAway() {
        assertThat(change("100.20", "100.00", VOLUME, " m³")).isEqualTo("▲ <1% vs this time yesterday");
    }

    @Test
    void theComparisonFollowsThePeriod() {
        assertThat(DashboardFigures.change(BigDecimal.TEN, BigDecimal.ONE, COUNT, "", DashboardPeriod.LAST_7_DAYS))
                .isEqualTo("▲ 900% vs the 7 days before");
    }

    @Test
    void theCountAxisStepsInWholeBatches() {
        assertThat(DashboardFigures.countAxisStep(0)).isEqualTo(1);
        assertThat(DashboardFigures.countAxisStep(3)).isEqualTo(1);
        assertThat(DashboardFigures.countAxisStep(7)).isEqualTo(2);
        assertThat(DashboardFigures.countAxisStep(23)).isEqualTo(10);
        assertThat(DashboardFigures.countAxisStep(180)).isEqualTo(50);
    }

    @Test
    void theCountAxisClearsThePeakAndIsNeverFlat() {
        assertThat(DashboardFigures.countAxisTop(0)).isEqualTo(1);
        assertThat(DashboardFigures.countAxisTop(2)).isEqualTo(3);
        assertThat(DashboardFigures.countAxisTop(7)).isEqualTo(8);
        assertThat(DashboardFigures.countAxisTop(8)).isEqualTo(10);
        assertThat(DashboardFigures.countAxisTop(23)).isEqualTo(30);
    }

    private static String change(String current, String previous, DecimalFormat format, String unit) {
        return DashboardFigures.change(new BigDecimal(current), new BigDecimal(previous), format, unit,
                DashboardPeriod.TODAY);
    }
}
