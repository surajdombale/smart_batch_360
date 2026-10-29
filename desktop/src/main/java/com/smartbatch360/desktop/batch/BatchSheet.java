package com.smartbatch360.desktop.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The grid of one batch's report - cycles down, materials across - worked out
 * once, so the printed sheet and the View dialog cannot show different figures.
 *
 * Above the cycles, "Target" and "Set" are what ONE cycle aims at. Below them,
 * "Achieved", "Set" and "Var %" are the whole load.
 */
public final class BatchSheet {

    private static final DateTimeFormatter CYCLE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final BigDecimal KG_PER_TONNE = new BigDecimal("1000");

    /** One line of the grid: shaded lines are the headings and totals, plain lines the cycles. */
    public record Row(List<String> cells, boolean shaded) {
    }

    /**
     * One material's figures, as numbers, for a report that formats them itself.
     *
     * @param perCycleTarget   what one cycle aims at
     * @param perCycleSetpoint what one cycle is set to
     * @param targetTotal      what the whole load was planned to need; null for a material it never planned
     * @param setpointTotal    what the machine was set to across the cycles recorded
     * @param achievedTotal    what was weighed across them
     */
    public record MaterialFigures(String name, BigDecimal perCycleTarget, BigDecimal perCycleSetpoint,
                                  BigDecimal targetTotal, BigDecimal setpointTotal, BigDecimal achievedTotal) {

        /** Weighed beyond the load's target, or short of it when negative; null with no target to compare. */
        public BigDecimal difference() {
            return targetTotal == null ? null : achievedTotal.subtract(targetTotal);
        }

        /** The difference as a percentage of the target. */
        public BigDecimal differencePercent() {
            return percentOf(difference(), targetTotal);
        }
    }

    /** One cycle, with what each material weighed in the order of materials(); null where it reported none. */
    public record CycleFigures(Integer number, Instant time, List<BigDecimal> weighed, BigDecimal total) {
    }

    /** What one material adds up to across the batch. */
    private static final class MaterialColumn {
        private final String name;
        private BigDecimal perCycleTarget;
        private BigDecimal perCycleSetpoint;
        private BigDecimal plannedSetpoint;
        private BigDecimal setTotal;
        private BigDecimal setpointTotal = BigDecimal.ZERO;
        private BigDecimal achievedTotal = BigDecimal.ZERO;

        private MaterialColumn(String name) {
            this.name = name;
        }
    }

    private final Map<String, MaterialColumn> materials;
    private final List<BatchCycleDto> cycles;

    private BatchSheet(Map<String, MaterialColumn> materials, List<BatchCycleDto> cycles) {
        this.materials = materials;
        this.cycles = cycles;
    }

    public static BatchSheet of(BatchDto batch, List<BatchCycleDto> cycles) {
        return new BatchSheet(materialColumns(batch, cycles), cycles);
    }

    public int materialCount() {
        return materials.size();
    }

    public boolean hasCycles() {
        return !cycles.isEmpty();
    }

    public Row headings() {
        List<String> cells = new ArrayList<>(List.of("No", "Time"));
        materials.values().forEach(m -> cells.add(m.name));
        cells.add("TOTAL");
        return new Row(cells, true);
    }

    /** Every line under the headings, top to bottom. */
    public List<Row> body() {
        List<Row> rows = new ArrayList<>(perCycleRows());
        rows.addAll(cycleRows());
        rows.addAll(totalRows());
        return rows;
    }

    /** "Target" and "Set" above the cycles: what ONE cycle aims at. */
    public List<Row> perCycleRows() {
        List<String> target = new ArrayList<>(List.of("Target", ""));
        List<String> set = new ArrayList<>(List.of("Set", ""));
        BigDecimal targetTotal = null;
        BigDecimal setTotal = null;
        for (MaterialColumn material : materials.values()) {
            target.add(number(material.perCycleTarget));
            set.add(number(material.perCycleSetpoint));
            targetTotal = add(targetTotal, material.perCycleTarget);
            setTotal = add(setTotal, material.perCycleSetpoint);
        }
        target.add(number(targetTotal));
        set.add(number(setTotal));
        return List.of(new Row(target, true), new Row(set, true));
    }

    public List<Row> cycleRows() {
        List<Row> rows = new ArrayList<>();
        for (BatchCycleDto cycle : cycles) {
            Map<String, BigDecimal> weighed = new LinkedHashMap<>();
            if (cycle.materials() != null) {
                for (BatchCycleDto.Material material : cycle.materials()) {
                    weighed.put(key(material.materialName()), material.achieved());
                }
            }
            List<String> cells = new ArrayList<>();
            cells.add(cycle.cycleNumber() == null ? "" : String.valueOf(cycle.cycleNumber()));
            cells.add(cycle.cycleTime() == null ? ""
                    : CYCLE_TIME.format(LocalDateTime.ofInstant(cycle.cycleTime(), ZoneId.systemDefault())));
            BigDecimal total = null;
            for (String materialKey : materials.keySet()) {
                BigDecimal value = weighed.get(materialKey);
                cells.add(number(value));
                total = add(total, value);
            }
            cells.add(number(total));
            rows.add(new Row(cells, false));
        }
        return rows;
    }

    /** "Achieved", "Set" and "Var %" under the cycles: the whole load. */
    public List<Row> totalRows() {
        List<String> achieved = new ArrayList<>(List.of("Achieved", ""));
        List<String> set = new ArrayList<>(List.of("Set", ""));
        List<String> variance = new ArrayList<>(List.of("Var %", ""));
        BigDecimal achievedTotal = BigDecimal.ZERO;
        BigDecimal setTotal = null;
        for (MaterialColumn material : materials.values()) {
            achieved.add(number(material.achievedTotal));
            set.add(number(material.setTotal));
            variance.add(variancePercent(material.achievedTotal, material.setTotal));
            achievedTotal = achievedTotal.add(material.achievedTotal);
            setTotal = add(setTotal, material.setTotal);
        }
        achieved.add(number(achievedTotal));
        set.add(number(setTotal));
        variance.add(variancePercent(achievedTotal, setTotal));
        return List.of(new Row(achieved, true), new Row(set, true), new Row(variance, true));
    }

    public List<MaterialFigures> materials() {
        return materials.values().stream()
                .map(m -> new MaterialFigures(m.name, m.perCycleTarget, m.perCycleSetpoint, m.setTotal,
                        m.setpointTotal, m.achievedTotal))
                .toList();
    }

    public List<CycleFigures> cycles() {
        List<CycleFigures> figures = new ArrayList<>();
        for (BatchCycleDto cycle : cycles) {
            Map<String, BigDecimal> weighed = new LinkedHashMap<>();
            if (cycle.materials() != null) {
                for (BatchCycleDto.Material material : cycle.materials()) {
                    weighed.put(key(material.materialName()), material.achieved());
                }
            }
            List<BigDecimal> values = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (String materialKey : materials.keySet()) {
                BigDecimal value = weighed.get(materialKey);
                values.add(value);
                total = value == null ? total : total.add(value);
            }
            figures.add(new CycleFigures(cycle.cycleNumber(), cycle.cycleTime(), values, total));
        }
        return figures;
    }

    /** Everything the cycles weighed, in tonnes - the sheet's "Actual Produced Quantity". */
    public String producedTonnes() {
        BigDecimal kilograms = BigDecimal.ZERO;
        for (MaterialColumn material : materials.values()) {
            kilograms = kilograms.add(material.achievedTotal);
        }
        return kilograms.divide(KG_PER_TONNE, 2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * The batch's own materials first, in the order the recipe lists them, then
     * anything a cycle reported that the batch never planned - it was weighed,
     * so it is printed, but it has no setpoint to compare against.
     */
    private static Map<String, MaterialColumn> materialColumns(BatchDto batch, List<BatchCycleDto> cycles) {
        Map<String, MaterialColumn> columns = new LinkedHashMap<>();
        if (batch.materials() != null) {
            for (BatchMaterialDto material : batch.materials()) {
                MaterialColumn column = columns.computeIfAbsent(key(material.materialName()),
                        k -> new MaterialColumn(material.materialName()));
                column.perCycleTarget = material.setpoint();
                column.perCycleSetpoint = material.setpoint();
                column.plannedSetpoint = material.setpoint();
                column.setTotal = material.target();
            }
        }
        for (BatchCycleDto cycle : cycles) {
            if (cycle.materials() == null) {
                continue;
            }
            for (BatchCycleDto.Material material : cycle.materials()) {
                MaterialColumn column = columns.computeIfAbsent(key(material.materialName()),
                        k -> new MaterialColumn(material.materialName()));
                if (material.achieved() != null) {
                    column.achievedTotal = column.achievedTotal.add(material.achieved());
                }
                // A cycle that does not say what it was set to was set to the plan.
                BigDecimal setTo = material.setpoint() != null ? material.setpoint() : column.plannedSetpoint;
                if (setTo != null) {
                    column.setpointTotal = column.setpointTotal.add(setTo);
                }
                // A cycle that carries its own target and setpoint says so
                // better than the plan does; the first one stands for the row.
                if (cycle == cycles.get(0)) {
                    if (material.target() != null) {
                        column.perCycleTarget = material.target();
                    }
                    if (material.setpoint() != null) {
                        column.perCycleSetpoint = material.setpoint();
                    }
                }
            }
        }
        return columns;
    }

    /** How far what was weighed is from what was set, as a percentage of what was set. */
    private static String variancePercent(BigDecimal achieved, BigDecimal set) {
        BigDecimal percent = percentOf(achieved == null || set == null ? null : achieved.subtract(set), set);
        return percent == null ? "" : percent.toPlainString();
    }

    private static BigDecimal percentOf(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 2, RoundingMode.HALF_UP);
    }

    private static String key(String materialName) {
        return materialName == null ? "" : materialName.trim().toLowerCase(Locale.ROOT);
    }

    private static BigDecimal add(BigDecimal total, BigDecimal value) {
        if (value == null) {
            return total;
        }
        return total == null ? value : total.add(value);
    }

    private static String number(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
