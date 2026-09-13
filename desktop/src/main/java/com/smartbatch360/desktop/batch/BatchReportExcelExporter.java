package com.smartbatch360.desktop.batch;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Writes the Batch Reports list to an Excel workbook - the second half of the
 * export pass unblocked 2026-09-12.
 *
 * The point of a spreadsheet over the PDF is that the figures stay figures, so
 * quantities are written as real numeric cells and cycle times as real dates,
 * never as the formatted strings the PDF prints. That is what lets someone
 * sort, filter and total them. Three sheets:
 * <ul>
 *   <li><b>Batches</b> - one row per batch, header frozen, autofilter on.</li>
 *   <li><b>Materials</b> - one row per material per batch, with the variance.</li>
 *   <li><b>About</b> - when it was generated and what the filters were. Kept on
 *       its own sheet so the tables start at row 1 and stay machine-friendly.</li>
 * </ul>
 *
 * fastexcel rather than Apache POI: this only ever writes, and POI's .xlsx
 * support would ship several MB of reading/formula machinery in every install.
 */
public final class BatchReportExcelExporter {

    private static final String QUANTITY_FORMAT = "#,##0.00";
    private static final String DATE_TIME_FORMAT = "dd mmm yyyy hh:mm";
    private static final String HEADER_FILL = "E8EBF0";

    private static final List<String> BATCH_HEADINGS = List.of(
            "Batch Number", "Cycle Date/Time", "Recipe", "Client", "Site", "Vehicle", "Driver",
            "Shift", "Cycle #", "Target (kg)", "Produced (kg)", "Remaining (kg)", "Status", "Order #");

    private static final List<String> MATERIAL_HEADINGS = List.of(
            "Batch Number", "Material", "Target", "Setpoint", "Achieved", "Variance", "Unit");

    /**
     * @param filterSummary human-readable description of the filters in force, recorded on the About sheet
     * @param note          optional extra line (e.g. a row cap that was hit), or null
     */
    public void write(File file, List<BatchDto> batches, String filterSummary, String note) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            Workbook workbook = new Workbook(out, "SmartBatch360", "4.0");
            writeBatches(workbook.newWorksheet("Batches"), batches);
            writeMaterials(workbook.newWorksheet("Materials"), batches);
            writeAbout(workbook.newWorksheet("About"), batches.size(), filterSummary, note);
            workbook.finish();
        }
    }

    private void writeBatches(Worksheet sheet, List<BatchDto> batches) {
        writeHeadings(sheet, BATCH_HEADINGS);

        int row = 1;
        for (BatchDto batch : batches) {
            int col = 0;
            text(sheet, row, col++, batch.batchNumber());
            if (batch.cycleDateTime() != null) {
                sheet.value(row, col, LocalDateTime.ofInstant(batch.cycleDateTime(), ZoneId.systemDefault()));
                sheet.style(row, col).format(DATE_TIME_FORMAT).set();
            }
            col++;
            text(sheet, row, col++, batch.recipeName());
            text(sheet, row, col++, batch.clientName());
            text(sheet, row, col++, batch.siteName());
            text(sheet, row, col++, batch.vehicleNumber());
            text(sheet, row, col++, batch.driverName());
            text(sheet, row, col++, batch.shift());
            if (batch.cycleNumber() != null) {
                sheet.value(row, col, batch.cycleNumber());
            }
            col++;
            quantity(sheet, row, col++, batch.targetQuantity());
            quantity(sheet, row, col++, batch.producedQuantity());
            quantity(sheet, row, col++, batch.remainingQuantity());
            text(sheet, row, col++, batch.status() == null ? null : batch.status().name());
            if (batch.orderId() != null) {
                sheet.value(row, col, batch.orderId());
            }
            row++;
        }

        double[] widths = {14, 18, 16, 16, 14, 14, 16, 8, 8, 12, 13, 14, 13, 9};
        for (int i = 0; i < widths.length; i++) {
            sheet.width(i, widths[i]);
        }
        finishTable(sheet, BATCH_HEADINGS.size());
    }

    private void writeMaterials(Worksheet sheet, List<BatchDto> batches) {
        writeHeadings(sheet, MATERIAL_HEADINGS);

        int row = 1;
        for (BatchDto batch : batches) {
            if (batch.materials() == null) {
                continue;
            }
            for (BatchMaterialDto material : batch.materials()) {
                text(sheet, row, 0, batch.batchNumber());
                text(sheet, row, 1, material.materialName());
                quantity(sheet, row, 2, material.target());
                quantity(sheet, row, 3, material.setpoint());
                quantity(sheet, row, 4, material.achieved());
                if (material.target() != null && material.achieved() != null) {
                    // Positive = more used than planned, matching the sign the
                    // Material Consumption screen already uses.
                    quantity(sheet, row, 5, material.achieved().subtract(material.target()));
                }
                text(sheet, row, 6, material.unit());
                row++;
            }
        }

        double[] widths = {14, 20, 12, 12, 12, 12, 7};
        for (int i = 0; i < widths.length; i++) {
            sheet.width(i, widths[i]);
        }
        finishTable(sheet, MATERIAL_HEADINGS.size());
    }

    private void writeAbout(Worksheet sheet, int batchCount, String filterSummary, String note) {
        int row = 0;
        label(sheet, row, "Report");
        text(sheet, row++, 1, "Batch Reports");
        label(sheet, row, "Generated");
        sheet.value(row, 1, LocalDateTime.now());
        sheet.style(row++, 1).format(DATE_TIME_FORMAT).set();
        label(sheet, row, "Filters");
        text(sheet, row++, 1, filterSummary);
        label(sheet, row, "Batches");
        sheet.value(row++, 1, batchCount);
        if (note != null) {
            label(sheet, row, "Note");
            text(sheet, row, 1, note);
        }
        sheet.width(0, 12);
        sheet.width(1, 90);
    }

    private void writeHeadings(Worksheet sheet, List<String> headings) {
        for (int col = 0; col < headings.size(); col++) {
            sheet.value(0, col, headings.get(col));
            sheet.style(0, col).bold().fillColor(HEADER_FILL).set();
        }
    }

    /** Header stays in view while scrolling, and every column can be filtered. */
    private void finishTable(Worksheet sheet, int columnCount) {
        sheet.freezePane(0, 1);
        sheet.setAutoFilter(0, 0, columnCount - 1);
    }

    private void label(Worksheet sheet, int row, String value) {
        sheet.value(row, 0, value);
        sheet.style(row, 0).bold().set();
    }

    private void quantity(Worksheet sheet, int row, int col, BigDecimal value) {
        if (value == null) {
            return;
        }
        sheet.value(row, col, value);
        sheet.style(row, col).format(QUANTITY_FORMAT).set();
    }

    private void text(Worksheet sheet, int row, int col, String value) {
        if (value != null) {
            sheet.value(row, col, sanitize(value));
        }
    }

    /**
     * XML 1.0 - which is what an .xlsx is inside - cannot hold control
     * characters at all, and one arriving in a customer or site name would make
     * Excel refuse the whole file as corrupt. Tab, newline and carriage return
     * are legal and kept.
     */
    private String sanitize(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
