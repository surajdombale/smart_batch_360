package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.header.HeaderDto;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One batch's report as a spreadsheet - the same document BatchSheetPdfExporter
 * draws, for a reader who wants to work with the numbers rather than print them.
 *
 * The grid is the point: cycles down, materials across, with the recipe's target
 * and the machine's setpoint above and the achieved totals and variance below.
 * Written as real numbers and dates, not text, so the totals can be checked and
 * the columns sorted - the same rule the list export follows.
 */
public class BatchSheetExcelExporter {

    private static final String DATE_TIME_FORMAT = "yyyy-mm-dd hh:mm:ss";
    private static final String TIME_FORMAT = "hh:mm:ss";

    public void write(File file, BatchDto batch, List<BatchCycleDto> cycles, HeaderDto company) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            Workbook workbook = new Workbook(out, "SmartBatch360", "7.0");
            writeReport(workbook.newWorksheet("Batch Report"), batch, cycles, company);
            workbook.finish();
        }
    }

    private void writeReport(Worksheet sheet, BatchDto batch, List<BatchCycleDto> cycles, HeaderDto company) {
        int row = 0;

        if (company != null) {
            sheet.value(row, 0, text(company.companyName()));
            sheet.style(row, 0).bold().fontSize(14).set();
            row++;
            String address = joinNonBlank(company.address(), company.city(), company.pinCode());
            if (!address.isBlank()) {
                sheet.value(row++, 0, address);
            }
            if (company.gstin() != null && !company.gstin().isBlank()) {
                sheet.value(row++, 0, "GST No: " + company.gstin());
            }
            row++;
        }

        sheet.value(row, 0, "Batch Report");
        sheet.style(row, 0).bold().fontSize(12).set();
        row += 2;

        row = detail(sheet, row, "Batch No", text(batch.batchNumber()), "Recipe", text(batch.recipeName()));
        row = detail(sheet, row, "Customer", text(batch.clientName()), "Site", text(batch.siteName()));
        row = detail(sheet, row, "Vehicle", text(batch.vehicleNumber()), "Driver", text(batch.driverName()));

        sheet.value(row, 0, "Date/Time");
        if (batch.cycleDateTime() != null) {
            sheet.value(row, 1, LocalDateTime.ofInstant(batch.cycleDateTime(), ZoneId.systemDefault()));
            sheet.style(row, 1).format(DATE_TIME_FORMAT).set();
        }
        sheet.value(row, 3, "Batch Quantity");
        if (batch.targetQuantity() != null) {
            sheet.value(row, 4, batch.targetQuantity());
        }
        row += 2;

        // Materials become columns, in the order the batch records them, with any
        // a cycle reports that the batch did not appended rather than dropped.
        List<String> materials = materialColumns(batch, cycles);

        sheet.value(row, 0, "Cycle");
        sheet.value(row, 1, "Time");
        for (int i = 0; i < materials.size(); i++) {
            sheet.value(row, 2 + i, materials.get(i));
        }
        sheet.value(row, 2 + materials.size(), "Total");
        sheet.range(row, 0, row, 2 + materials.size()).style().bold().set();
        int headerRow = row;
        row++;

        row = materialRow(sheet, row, "Target", materials, name -> targetOf(batch, name));
        row = materialRow(sheet, row, "Setpoint", materials, name -> setpointOf(batch, name));

        int firstCycleRow = row;
        for (BatchCycleDto cycle : cycles) {
            sheet.value(row, 0, cycle.cycleNumber());
            if (cycle.cycleTime() != null) {
                sheet.value(row, 1, LocalDateTime.ofInstant(cycle.cycleTime(), ZoneId.systemDefault()));
                sheet.style(row, 1).format(TIME_FORMAT).set();
            }
            for (int i = 0; i < materials.size(); i++) {
                BigDecimal achieved = achievedOf(cycle, materials.get(i));
                if (achieved != null) {
                    sheet.value(row, 2 + i, achieved);
                }
            }
            if (cycle.totalAchieved() != null) {
                sheet.value(row, 2 + materials.size(), cycle.totalAchieved());
            }
            row++;
        }

        if (!cycles.isEmpty()) {
            row = totalsRow(sheet, row, "Achieved total", materials, firstCycleRow, row - 1);
            row = materialRow(sheet, row, "Set total", materials,
                    name -> multiply(setpointOf(batch, name), cycles.size()));
            row = varianceRow(sheet, row, materials, row - 2, row - 1);
        }

        sheet.width(0, 14);
        sheet.width(1, 12);
        for (int i = 0; i < materials.size() + 1; i++) {
            sheet.width(2 + i, 14);
        }
        sheet.freezePane(0, headerRow + 1);
    }

    private int detail(Worksheet sheet, int row, String leftLabel, String leftValue,
                       String rightLabel, String rightValue) {
        sheet.value(row, 0, leftLabel);
        sheet.value(row, 1, leftValue);
        sheet.value(row, 3, rightLabel);
        sheet.value(row, 4, rightValue);
        return row + 1;
    }

    private int materialRow(Worksheet sheet, int row, String label, List<String> materials,
                            java.util.function.Function<String, BigDecimal> value) {
        sheet.value(row, 0, label);
        sheet.style(row, 0).bold().set();
        for (int i = 0; i < materials.size(); i++) {
            BigDecimal figure = value.apply(materials.get(i));
            if (figure != null) {
                sheet.value(row, 2 + i, figure);
            }
        }
        return row + 1;
    }

    /**
     * Summed with a formula rather than a number, so a reader can see where the
     * figure comes from and the sheet re-totals if a cell is corrected.
     */
    private int totalsRow(Worksheet sheet, int row, String label, List<String> materials,
                          int firstCycleRow, int lastCycleRow) {
        sheet.value(row, 0, label);
        sheet.style(row, 0).bold().set();
        for (int i = 0; i < materials.size(); i++) {
            String column = columnLetter(2 + i);
            sheet.formula(row, 2 + i, "SUM(" + column + (firstCycleRow + 1) + ":" + column + (lastCycleRow + 1) + ")");
        }
        return row + 1;
    }

    private int varianceRow(Worksheet sheet, int row, List<String> materials, int achievedRow, int setRow) {
        sheet.value(row, 0, "% variance");
        sheet.style(row, 0).bold().set();
        for (int i = 0; i < materials.size(); i++) {
            String column = columnLetter(2 + i);
            String achieved = column + (achievedRow + 1);
            String set = column + (setRow + 1);
            // Guarded: a material with no setpoint would otherwise divide by zero.
            sheet.formula(row, 2 + i, "IF(" + set + "=0,\"\",(" + achieved + "-" + set + ")/" + set + "*100)");
            sheet.style(row, 2 + i).format("0.00").set();
        }
        return row + 1;
    }

    private List<String> materialColumns(BatchDto batch, List<BatchCycleDto> cycles) {
        Set<String> names = new LinkedHashSet<>();
        if (batch.materials() != null) {
            batch.materials().forEach(m -> names.add(m.materialName()));
        }
        for (BatchCycleDto cycle : cycles) {
            if (cycle.materials() != null) {
                cycle.materials().forEach(m -> names.add(m.materialName()));
            }
        }
        return new ArrayList<>(names);
    }

    private BigDecimal targetOf(BatchDto batch, String materialName) {
        if (batch.materials() == null) {
            return null;
        }
        return batch.materials().stream()
                .filter(m -> materialName.equalsIgnoreCase(m.materialName()))
                .map(BatchMaterialDto::target)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal setpointOf(BatchDto batch, String materialName) {
        if (batch.materials() == null) {
            return null;
        }
        return batch.materials().stream()
                .filter(m -> materialName.equalsIgnoreCase(m.materialName()))
                .map(BatchMaterialDto::setpoint)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal achievedOf(BatchCycleDto cycle, String materialName) {
        if (cycle.materials() == null) {
            return null;
        }
        return cycle.materials().stream()
                .filter(m -> materialName.equalsIgnoreCase(m.materialName()))
                .map(BatchCycleDto.Material::achieved)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal multiply(BigDecimal value, int times) {
        return value == null ? null : value.multiply(BigDecimal.valueOf(times));
    }

    /** Spreadsheet column letters, for the formulas above. */
    private String columnLetter(int zeroBasedIndex) {
        StringBuilder letters = new StringBuilder();
        int index = zeroBasedIndex + 1;
        while (index > 0) {
            int remainder = (index - 1) % 26;
            letters.insert(0, (char) ('A' + remainder));
            index = (index - 1) / 26;
        }
        return letters.toString();
    }

    private String joinNonBlank(String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                kept.add(part.trim());
            }
        }
        return String.join(", ", kept);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
