package com.smartbatch360.api.materialconsumption;

import com.smartbatch360.api.batch.MaterialConsumptionRow;
import com.smartbatch360.api.batch.BatchMaterialRepository;
import com.smartbatch360.api.common.InvalidRequestException;
import com.smartbatch360.api.common.NotFoundException;
import com.smartbatch360.api.common.ReportingZone;
import com.smartbatch360.api.order.SalesOrder;
import com.smartbatch360.api.order.SalesOrderRepository;
import com.smartbatch360.api.recipe.RecipeMaterial;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Material Consumption (docs/02_UI_REFERENCE.md's "Material Consumption
 * reference"): target vs achieved vs variance, aggregated by material name
 * and day/week/month. Built entirely from existing Batch/BatchMaterial data
 * - no new entity needed. First-pass scope confirmed with the user
 * 2026-08-26: the aggregated table only; charts are a later pass.
 *
 * Aggregation happens in Java rather than a grouped SQL query so the date
 * bucketing (day/week/month) stays portable across MySQL (runtime) and H2
 * (tests) instead of relying on MySQL-specific date functions. The rows it
 * aggregates are projections, not entities: with no date filter this covers
 * every batch ever made, and building the entity graph for that cost seconds.
 *
 * Two different questions are answered here, deliberately kept separate:
 *  - search(...)      : what production ACTUALLY consumed, from batch history.
 *  - forOrder(...)    : what an order WILL consume, projected from its recipe
 *                       (added 2026-08-27 with the Order flow).
 */
@Service
@Transactional(readOnly = true)
public class MaterialConsumptionService {

    /** Scale for the ordered-vs-batch ratio; wider than any persisted quantity so it doesn't skew the result. */
    private static final int RATIO_SCALE = 10;

    /** Reported material quantities: 2dp matches how quantities are stored and displayed elsewhere. */
    private static final int QUANTITY_SCALE = 2;

    private final BatchMaterialRepository batchMaterialRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ReportingZone reportingZone;

    public MaterialConsumptionService(BatchMaterialRepository batchMaterialRepository,
                                       SalesOrderRepository salesOrderRepository,
                                       ReportingZone reportingZone) {
        this.batchMaterialRepository = batchMaterialRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.reportingZone = reportingZone;
    }

    /**
     * Projects an order's material consumption: Order -> Recipe -> Recipe
     * Materials -> Material. The recipe defines quantities for one batch of
     * recipeBatchQuantityKg, so an order for N kg consumes each material
     * scaled by N / recipeBatchQuantityKg.
     */
    public OrderConsumptionResponse forOrder(Long orderId) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> NotFoundException.forId("Order", orderId));

        BigDecimal batchQuantity = order.getRecipe().getTotalBatchQuantityKg();
        if (batchQuantity == null || batchQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Recipe '" + order.getRecipe().getName()
                    + "' has no usable batch quantity, so consumption for this order cannot be calculated.");
        }
        BigDecimal scale = order.getQuantityKg().divide(batchQuantity, RATIO_SCALE, RoundingMode.HALF_UP);

        List<OrderMaterialConsumptionResponse> materials = order.getRecipe().getMaterials().stream()
                .map(line -> toConsumption(line, scale))
                .toList();

        return new OrderConsumptionResponse(
                order.getId(),
                order.getClient().getName(),
                order.getSite().getName(),
                order.getRecipe().getId(),
                order.getRecipe().getName(),
                order.getQuantityKg(),
                batchQuantity,
                materials);
    }

    private OrderMaterialConsumptionResponse toConsumption(RecipeMaterial line, BigDecimal scale) {
        return new OrderMaterialConsumptionResponse(
                line.getMaterial().getId(),
                line.getMaterial().getName(),
                line.getQuantity().multiply(scale).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
    }

    public List<MaterialConsumptionResponse> search(MaterialConsumptionSearchCriteria criteria) {
        Instant from = criteria.dateFrom() != null ? reportingZone.startOfDay(criteria.dateFrom()) : null;
        Instant to = criteria.dateTo() != null ? reportingZone.startOfNextDay(criteria.dateTo()) : null;
        String materialName = criteria.materialName() != null && !criteria.materialName().isBlank()
                ? criteria.materialName().trim() : null;
        MaterialConsumptionGroupBy groupBy = criteria.groupBy() != null
                ? criteria.groupBy() : MaterialConsumptionGroupBy.DAY;

        List<MaterialConsumptionRow> rows = batchMaterialRepository.findForConsumption(from, to, materialName);

        Map<GroupKey, Accumulator> grouped = new LinkedHashMap<>();
        for (MaterialConsumptionRow row : rows) {
            String period = periodLabel(row.cycleDateTime(), groupBy);
            GroupKey key = new GroupKey(row.materialName(), period);
            Accumulator acc = grouped.computeIfAbsent(key,
                    k -> new Accumulator(row.materialName(), period));
            acc.add(row.unit(), row.target(), row.achieved());
        }

        return grouped.values().stream()
                .map(Accumulator::toResponse)
                .sorted(Comparator.comparing(MaterialConsumptionResponse::period)
                        .thenComparing(MaterialConsumptionResponse::materialName))
                .toList();
    }

    private String periodLabel(Instant cycleDateTime, MaterialConsumptionGroupBy groupBy) {
        LocalDate date = reportingZone.dayOf(cycleDateTime);
        return switch (groupBy) {
            case DAY -> date.toString();
            case WEEK -> String.format("%d-W%02d",
                    date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTH -> String.format("%d-%02d", date.getYear(), date.getMonthValue());
        };
    }

    /**
     * What a row is grouped under. A record rather than the two values joined
     * into one string: the separator used to be a literal NUL character, which
     * made this file binary to git and grep - no reviewable diff, no matching
     * lines in a search - for a delimiter that is not needed at all.
     */
    private record GroupKey(String materialName, String period) {
    }

    private static final class Accumulator {
        private final String materialName;
        private final String period;
        /**
         * How many rows carried each unit. The unit used to be whichever row
         * happened to create the group, so a period whose rows disagree - the
         * plant has pre-kilogram history, where one water line still says "L" -
         * reported a different unit depending on the order the database
         * returned rows in. Counting makes it the same every time.
         */
        private final Map<String, Long> unitCounts = new TreeMap<>();
        private BigDecimal totalTarget = BigDecimal.ZERO;
        private BigDecimal totalAchieved = BigDecimal.ZERO;
        private long count;

        Accumulator(String materialName, String period) {
            this.materialName = materialName;
            this.period = period;
        }

        void add(String unit, BigDecimal target, BigDecimal achieved) {
            unitCounts.merge(unit == null ? "" : unit, 1L, Long::sum);
            totalTarget = totalTarget.add(target != null ? target : BigDecimal.ZERO);
            totalAchieved = totalAchieved.add(achieved != null ? achieved : BigDecimal.ZERO);
            count++;
        }

        MaterialConsumptionResponse toResponse() {
            return new MaterialConsumptionResponse(materialName, dominantUnit(), period, totalTarget,
                    totalAchieved, totalAchieved.subtract(totalTarget), count);
        }

        /** The unit most of the rows agree on; ties go to the first alphabetically. */
        private String dominantUnit() {
            return unitCounts.entrySet().stream()
                    .max(Comparator.comparingLong(Map.Entry<String, Long>::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }
    }
}
