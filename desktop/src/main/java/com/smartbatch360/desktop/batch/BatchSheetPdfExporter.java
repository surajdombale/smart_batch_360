package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.header.HeaderDto;
import com.smartbatch360.desktop.header.HeaderLogoDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
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
 * The report for ONE batch: the company's letterhead, the batch's own details,
 * and a grid of what every mixer cycle weighed against what it was set to.
 *
 * Laid out after the reference plant's batch report (sample "101.pdf",
 * 06-Oct-2026). BatchReportPdfExporter beside this writes the LIST of batches;
 * this is the sheet that goes with one load.
 *
 * Two lines of that sample are left out on purpose - "BOM" and "Batch Hold
 * Time". Neither is recorded anywhere, and a printed zero would read as a
 * measurement.
 */
public final class BatchSheetPdfExporter {

    /** Landscape A4, like the sample. */
    private static final PDRectangle PAGE_SIZE =
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

    private static final float MARGIN = 20;
    private static final float ROW_HEIGHT = 17;
    private static final float LABEL_COLUMN_WIDTH = 50;
    private static final float TIME_COLUMN_WIDTH = 50;
    private static final float LOGO_BOX_WIDTH = 160;
    private static final float LETTERHEAD_HEIGHT = 66;
    private static final float FOOTER_SPACE = 30;
    private static final float GREY = 0.753f;

    private static final DateTimeFormatter BATCH_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter CYCLE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final BigDecimal KG_PER_TONNE = new BigDecimal("1000");

    private final PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** One row of the grid: shaded rows are the headings and totals, plain rows the cycles. */
    private record Row(List<String> cells, boolean shaded) {
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

    /**
     * @param company the company row to print as the letterhead, or null to leave it blank
     * @param logo    its logo, or null if none has been uploaded
     */
    public void write(File file, BatchDto batch, List<BatchCycleDto> cycles, HeaderDto company, HeaderLogoDto logo)
            throws IOException {
        try (PDDocument document = build(batch, cycles, company, logo)) {
            document.save(file);
        }
    }

    /** The report as an open document. The caller owns it and must close it. */
    public PDDocument build(BatchDto batch, List<BatchCycleDto> cycles, HeaderDto company, HeaderLogoDto logo)
            throws IOException {
        Map<String, MaterialColumn> materials = materialColumns(batch, cycles);
        List<Row> cycleRows = cycleRows(cycles, materials);
        List<Row> totalRows = totalRows(materials);
        Row headings = headingRow(materials);
        float[] widths = columnWidths(materials.size());
        float fontSize = materials.size() > 14 ? 6.5f : 8;

        PDDocument document = new PDDocument();
        try {
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            try {
                float y = drawFirstPageTop(document, content, batch, company, logo);

                y = drawRow(content, y, headings, widths, fontSize);
                for (Row row : perCycleRows(materials)) {
                    y = drawRow(content, y, row, widths, fontSize);
                }
                for (Row row : cycleRows) {
                    if (y - ROW_HEIGHT < MARGIN + FOOTER_SPACE) {
                        content.close();
                        page = new PDPage(PAGE_SIZE);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = drawContinuationTop(content, batch);
                        y = drawRow(content, y, headings, widths, fontSize);
                    }
                    y = drawRow(content, y, row, widths, fontSize);
                }

                // The totals and the produced line stay together: a page that
                // ends on "Achieved" with "Set" overleaf cannot be compared.
                float totalsHeight = (totalRows.size() * ROW_HEIGHT) + 25;
                if (y - totalsHeight < MARGIN + FOOTER_SPACE) {
                    content.close();
                    page = new PDPage(PAGE_SIZE);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = drawContinuationTop(content, batch);
                    y = drawRow(content, y, headings, widths, fontSize);
                }
                for (Row row : totalRows) {
                    y = drawRow(content, y, row, widths, fontSize);
                }
                if (cycles.isEmpty()) {
                    drawText(content, bodyFont, 9, MARGIN + 4, y - 14,
                            "No cycles have been recorded for this batch yet.");
                    y -= 14;
                }
                drawText(content, boldFont, 9, MARGIN + 4, y - 20,
                        "Actual Produced Quantity : " + tonnes(materials) + " TON");
            } finally {
                content.close();
            }

            drawPageNumbers(document);
            return document;
        } catch (IOException | RuntimeException e) {
            // Handed back open on success, so on failure it is ours to close.
            document.close();
            throw e;
        }
    }

    // ---------------------------------------------------------------- the grid

    /**
     * The batch's own materials first, in the order the recipe lists them, then
     * anything a cycle reported that the batch never planned - it was weighed,
     * so it is printed, but it has no setpoint to compare against.
     */
    private Map<String, MaterialColumn> materialColumns(BatchDto batch, List<BatchCycleDto> cycles) {
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

    private Row headingRow(Map<String, MaterialColumn> materials) {
        List<String> cells = new ArrayList<>(List.of("No", "Time"));
        materials.values().forEach(m -> cells.add(m.name));
        cells.add("TOTAL");
        return new Row(cells, true);
    }

    /** "Target" and "Set" above the cycles: what ONE cycle aims at. */
    private List<Row> perCycleRows(Map<String, MaterialColumn> materials) {
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

    private List<Row> cycleRows(List<BatchCycleDto> cycles, Map<String, MaterialColumn> materials) {
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
    private List<Row> totalRows(Map<String, MaterialColumn> materials) {
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

    /** How far what was weighed is from what was set, as a percentage of what was set. */
    private String variancePercent(BigDecimal achieved, BigDecimal set) {
        if (set == null || set.signum() == 0) {
            return "";
        }
        return achieved.subtract(set).multiply(BigDecimal.valueOf(100))
                .divide(set, 2, RoundingMode.HALF_UP).toPlainString();
    }

    private String tonnes(Map<String, MaterialColumn> materials) {
        BigDecimal kilograms = BigDecimal.ZERO;
        for (MaterialColumn material : materials.values()) {
            kilograms = kilograms.add(material.achievedTotal);
        }
        return kilograms.divide(KG_PER_TONNE, 2, RoundingMode.HALF_UP).toPlainString();
    }

    private float[] columnWidths(int materialCount) {
        float usable = PAGE_SIZE.getWidth() - (2 * MARGIN) - LABEL_COLUMN_WIDTH - TIME_COLUMN_WIDTH;
        float each = usable / (materialCount + 1);
        float[] widths = new float[materialCount + 3];
        widths[0] = LABEL_COLUMN_WIDTH;
        widths[1] = TIME_COLUMN_WIDTH;
        for (int i = 2; i < widths.length; i++) {
            widths[i] = each;
        }
        return widths;
    }

    // ----------------------------------------------------------------- drawing

    /** @return the y the grid starts at */
    private float drawFirstPageTop(PDDocument document, PDPageContentStream content, BatchDto batch,
                                   HeaderDto company, HeaderLogoDto logo) throws IOException {
        float pageWidth = PAGE_SIZE.getWidth();
        float top = PAGE_SIZE.getHeight() - MARGIN;

        String title = "Batch Report";
        drawText(content, boldFont, 14, (pageWidth - stringWidth(title, boldFont, 14)) / 2, top - 12, title);

        float boxTop = top - 22;
        float boxBottom = boxTop - LETTERHEAD_HEIGHT;
        float textBoxWidth = pageWidth - (2 * MARGIN) - LOGO_BOX_WIDTH;
        content.setNonStrokingColor(GREY, GREY, GREY);
        content.addRect(MARGIN, boxBottom, pageWidth - (2 * MARGIN), LETTERHEAD_HEIGHT);
        content.fill();
        content.setNonStrokingColor(0f, 0f, 0f);

        String[] lines = {
                "Company Name : " + (company == null ? "" : text(company.companyName())),
                "Address : " + (company == null ? "" : text(company.address())),
                "City : " + (company == null ? "" : text(company.city())),
                "Pin Code : " + (company == null ? "" : text(company.pinCode())),
                "GST Number : " + (company == null ? "" : text(company.gstin()))};
        float lineY = boxTop - 14;
        for (String line : lines) {
            drawText(content, boldFont, 10, MARGIN + 6, lineY, truncate(line, boldFont, 10, textBoxWidth - 12));
            lineY -= 11;
        }
        drawLogo(document, content, logo, MARGIN + textBoxWidth, boxBottom);

        float rowY = boxBottom - 20;
        String batchDate = batch.cycleDateTime() == null ? ""
                : BATCH_DATE.format(LocalDateTime.ofInstant(batch.cycleDateTime(), ZoneId.systemDefault()));
        drawDetailRow(content, rowY, "Customer Name :", batch.clientName(), "Site Name :", batch.siteName());
        drawDetailRow(content, rowY - 17, "Batch Number :", batch.batchNumber(), "Batch Date :", batchDate);
        drawDetailRow(content, rowY - 34, "Driver Name :", batch.driverName(), "Recipe Name :", batch.recipeName());
        drawDetailRow(content, rowY - 51, "Vehicle Number :", batch.vehicleNumber(),
                "Batch Quantity :", cubicMetres(batch.batchSizeM3()));
        drawDetailRow(content, rowY - 68, "Order Number :", batch.orderId() == null ? "" : batch.orderId().toString(),
                "PerCycle Quantity :", cubicMetres(batch.perCycleM3()));
        return rowY - 68 - 22;
    }

    private float drawContinuationTop(PDPageContentStream content, BatchDto batch) throws IOException {
        float top = PAGE_SIZE.getHeight() - MARGIN;
        drawText(content, boldFont, 11, MARGIN, top - 12,
                truncate("Batch Report - Batch " + text(batch.batchNumber()) + " (continued)", boldFont, 11,
                        PAGE_SIZE.getWidth() - (2 * MARGIN)));
        return top - 24;
    }

    private void drawDetailRow(PDPageContentStream content, float y, String leftLabel, String leftValue,
                               String rightLabel, String rightValue) throws IOException {
        drawText(content, boldFont, 9, 24, y, leftLabel);
        drawText(content, bodyFont, 9, 184.4f, y, truncate(leftValue, bodyFont, 9, 230));
        drawText(content, boldFont, 9, 425, y, rightLabel);
        drawText(content, bodyFont, 9, 585.4f, y, truncate(rightValue, bodyFont, 9, 230));
    }

    /**
     * Scaled to fit its box, never stretched. A logo that cannot be decoded is
     * left out rather than failing the report - the batch's figures are what
     * the sheet is for.
     */
    private void drawLogo(PDDocument document, PDPageContentStream content, HeaderLogoDto logo, float boxLeft,
                          float boxBottom) {
        if (logo == null || logo.data() == null || logo.data().length == 0) {
            return;
        }
        try {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, logo.data(), "company-logo");
            float maxWidth = LOGO_BOX_WIDTH - 16;
            float maxHeight = LETTERHEAD_HEIGHT - 12;
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            content.drawImage(image, boxLeft + ((LOGO_BOX_WIDTH - width) / 2),
                    boxBottom + ((LETTERHEAD_HEIGHT - height) / 2), width, height);
        } catch (IOException | RuntimeException e) {
            // Deliberately nothing: see the method comment.
        }
    }

    /** @return the y of the next row */
    private float drawRow(PDPageContentStream content, float top, Row row, float[] widths, float fontSize)
            throws IOException {
        float bottom = top - ROW_HEIGHT;
        PDFont font = row.shaded() ? boldFont : bodyFont;
        float x = MARGIN;
        for (int i = 0; i < widths.length; i++) {
            if (row.shaded()) {
                content.setNonStrokingColor(GREY, GREY, GREY);
                content.addRect(x, bottom, widths[i], ROW_HEIGHT);
                content.fill();
                content.setNonStrokingColor(0f, 0f, 0f);
            }
            content.setLineWidth(0.5f);
            content.addRect(x, bottom, widths[i], ROW_HEIGHT);
            content.stroke();

            String value = truncate(row.cells().get(i), font, fontSize, widths[i] - 4);
            float textWidth = stringWidth(value, font, fontSize);
            drawText(content, font, fontSize, x + ((widths[i] - textWidth) / 2), bottom + 5, value);
            x += widths[i];
        }
        return bottom;
    }

    private void drawPageNumbers(PDDocument document) throws IOException {
        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(i),
                    PDPageContentStream.AppendMode.APPEND, true)) {
                String footer = "SmartBatch360  -  Page " + (i + 1) + " of " + total;
                drawText(content, bodyFont, 8,
                        PAGE_SIZE.getWidth() - MARGIN - stringWidth(footer, bodyFont, 8), MARGIN - 8, footer);
            }
        }
    }

    private void drawText(PDPageContentStream content, PDFont font, float size, float x, float y, String value)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(sanitize(value));
        content.endText();
    }

    // ----------------------------------------------------------------- helpers

    /** Cut to fit rather than let a long name run into the next cell. */
    private String truncate(String value, PDFont font, float size, float maxWidth) {
        String safe = sanitize(value);
        if (stringWidth(safe, font, size) <= maxWidth) {
            return safe;
        }
        String suffix = "..";
        StringBuilder builder = new StringBuilder(safe);
        while (builder.length() > 0 && stringWidth(builder + suffix, font, size) > maxWidth) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder + suffix;
    }

    private float stringWidth(String value, PDFont font, float size) {
        try {
            return font.getStringWidth(value) / 1000 * size;
        } catch (IOException | RuntimeException e) {
            return value.length() * size * 0.6f;
        }
    }

    /**
     * The standard-14 fonts are WinAnsi-encoded and showText throws on anything
     * outside it. A material or customer name with an unusual character must
     * not be able to fail the report, so replace what cannot be drawn.
     */
    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            builder.append(c == '\n' || c == '\r' || c == '\t' ? ' '
                    : (c >= 32 && c <= 126) || (c >= 161 && c <= 255) ? c : '?');
        }
        return builder.toString();
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

    private static String cubicMetres(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " m3";
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
