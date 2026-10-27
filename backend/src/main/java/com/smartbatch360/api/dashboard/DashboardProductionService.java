package com.smartbatch360.api.dashboard;

import com.smartbatch360.api.batch.BatchRepository;
import com.smartbatch360.api.batch.BatchTotals;
import com.smartbatch360.api.batch.ConcreteDensity;
import com.smartbatch360.api.common.ReportingZone;
import com.smartbatch360.api.dashboard.DashboardProductionResponse.RecentBatch;
import com.smartbatch360.api.dashboard.DashboardProductionResponse.Totals;
import com.smartbatch360.api.dashboard.DashboardProductionResponse.TrendPoint;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The production half of the dashboard: headline totals, the trend chart and
 * the latest batches, for one period. Four queries whatever the volume - the
 * totals twice, the cycle times once, the recent rows once.
 */
@Service
public class DashboardProductionService {

    /** As many as the reference dashboard lists. */
    static final int RECENT_BATCHES = 5;

    /** Volumes are shown to the hundredth of a cubic metre. */
    private static final int VOLUME_SCALE = 2;

    private final BatchRepository batchRepository;
    private final ReportingZone reportingZone;
    private final ConcreteDensity concreteDensity;

    public DashboardProductionService(BatchRepository batchRepository,
                                      ReportingZone reportingZone,
                                      ConcreteDensity concreteDensity) {
        this.batchRepository = batchRepository;
        this.reportingZone = reportingZone;
        this.concreteDensity = concreteDensity;
    }

    @Transactional(readOnly = true)
    public DashboardProductionResponse getProduction(DashboardPeriod period) {
        return getProduction(period, Instant.now());
    }

    /** As of a given moment, so a test can say when "now" is. */
    DashboardProductionResponse getProduction(DashboardPeriod period, Instant now) {
        LocalDate today = reportingZone.dayOf(now);
        LocalDate firstDay = period.firstDay(today);
        LocalDate lastDay = period.lastDay(today);
        Instant from = reportingZone.startOfDay(firstDay);
        Instant to = reportingZone.startOfNextDay(lastDay);

        Instant previousFrom = reportingZone.startOfDay(firstDay.minusDays(period.days()));
        Instant previousTo = period.endsToday() ? reportingZone.daysBefore(now, period.days()) : from;

        List<RecentBatch> recent = batchRepository
                .findRecentBetween(from, to, PageRequest.of(0, RECENT_BATCHES))
                .stream()
                .map(row -> new RecentBatch(row.id(), row.batchNumber(), row.recipeName(), row.vehicleNumber(),
                        cubicMetres(row.producedKg()), row.cycleDateTime(), row.status()))
                .toList();

        return new DashboardProductionResponse(
                period,
                firstDay,
                lastDay,
                totals(batchRepository.totalsBetween(from, to)),
                totals(batchRepository.totalsBetween(previousFrom, previousTo)),
                trend(period, firstDay, lastDay, now, batchRepository.findCycleDateTimesBetween(from, to)),
                recent);
    }

    private Totals totals(BatchTotals totals) {
        return new Totals(totals.batches(), cubicMetres(totals.producedKg()), totals.vehicles());
    }

    private BigDecimal cubicMetres(BigDecimal kilograms) {
        return concreteDensity.toCubicMetres(kilograms).setScale(VOLUME_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Every hour or day in the period gets a point, with the quiet ones at zero,
     * so the chart shows the gaps rather than drawing a line across them. Today
     * stops at the current hour: the hours still to come have not happened, and
     * plotting them as zero would show a slump that is not there.
     */
    private List<TrendPoint> trend(DashboardPeriod period, LocalDate firstDay, LocalDate lastDay,
                                   Instant now, List<Instant> cycleTimes) {
        Function<Instant, LocalDateTime> bucket = period.hourly()
                ? reportingZone::hourOf
                : instant -> reportingZone.dayOf(instant).atStartOfDay();
        Map<LocalDateTime, Long> counts = cycleTimes.stream()
                .collect(Collectors.groupingBy(bucket, Collectors.counting()));

        Set<LocalDateTime> buckets = new LinkedHashSet<>();
        if (period.hourly()) {
            Instant end = reportingZone.startOfNextDay(lastDay);
            if (period.endsToday() && now.isBefore(end)) {
                end = now;
            }
            for (Instant hour = reportingZone.startOfDay(firstDay); hour.isBefore(end);
                 hour = hour.plus(1, ChronoUnit.HOURS)) {
                buckets.add(reportingZone.hourOf(hour));
            }
        } else {
            for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
                buckets.add(day.atStartOfDay());
            }
        }

        List<TrendPoint> points = new ArrayList<>(buckets.size());
        for (LocalDateTime start : buckets) {
            points.add(new TrendPoint(start, counts.getOrDefault(start, 0L)));
        }
        return points;
    }
}
