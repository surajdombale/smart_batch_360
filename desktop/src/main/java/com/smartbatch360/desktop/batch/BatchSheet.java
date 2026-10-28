package com.smartbatch360.desktop.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** What one material adds up to across the batch. */
    private static final class MaterialColumn {
        private final String name;
        private BigDecimal perCycleTarget;
        private BigDecimal perCycleSetpoint;
        private BigDecimal setTotal;
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
        if (set == null || set.signum() == 0) {
            return "";
        }
        return achieved.subtract(set).multiply(BigDecimal.valueOf(100))
                .divide(set, 2, RoundingMode.HALF_UP).toPlainString();
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
