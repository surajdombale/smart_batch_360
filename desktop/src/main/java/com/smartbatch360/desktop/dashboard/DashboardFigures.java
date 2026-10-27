package com.smartbatch360.desktop.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** The wording and scaling behind the dashboard's numbers, kept apart from the JavaFX nodes so they can be tested. */
final class DashboardFigures {

    private DashboardFigures() {
    }

    /**
     * The line under a headline figure: which way it moved and by how much.
     * A percentage of nothing means nothing, so a rise from zero is given as the
     * amount instead.
     */
    static String change(BigDecimal current, BigDecimal previous, DecimalFormat format, String unit,
                         DashboardPeriod period) {
        int direction = current.compareTo(previous);
        if (direction == 0) {
            return "No change " + period.comparison();
        }
        String arrow = direction > 0 ? "▲ " : "▼ ";
        BigDecimal difference = current.subtract(previous).abs();
        if (previous.signum() == 0) {
            return arrow + "+" + format.format(difference) + unit + " " + period.comparison();
        }
        BigDecimal percent = difference.multiply(BigDecimal.valueOf(100)).divide(previous, 0, RoundingMode.HALF_UP);
        String amount = percent.signum() == 0 ? "<1%" : percent.toPlainString() + "%";
        return arrow + amount + " " + period.comparison();
    }

    /**
     * A whole-number step for the batch-count axis, at most four steps to the
     * top. Batches come in ones, so a step of half a batch would only print the
     * same label twice.
     */
    static long countAxisStep(long max) {
        for (long magnitude = 1; ; magnitude *= 10) {
            for (long multiple : new long[]{1, 2, 5}) {
                long step = multiple * magnitude;
                if (step * 4 >= max) {
                    return step;
                }
            }
        }
    }

    /**
     * The top of the batch-count axis: the first step above the peak, and never
     * zero. Strictly above - a peak on the top line had its marker cut in half.
     */
    static long countAxisTop(long max) {
        long step = countAxisStep(max);
        return step * (max / step + 1);
    }

    static String batches(long count) {
        return count == 1 ? "1 batch" : count + " batches";
    }
}
