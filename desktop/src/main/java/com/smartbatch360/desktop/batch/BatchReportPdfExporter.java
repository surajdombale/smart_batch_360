package com.smartbatch360.desktop.batch;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Writes the Batch Reports list to a PDF - the export deferred since
 * 2026-08-24 and unblocked by the user 2026-09-12.
 *
 * Built on PDFBox rather than OpenPDF deliberately: OpenPDF has the friendlier
 * table API, but it is LGPL and this application is delivered to a customer,
 * so the Apache-2.0 library wins and the table is drawn by hand here.
 *
 * The report is whatever the screen's filters currently select, not just the
 * visible page, so what you export matches what you searched for.
 */
public final class BatchReportPdfExporter {

    /** Landscape A4 - nine columns do not fit portrait. */
    private static final PDRectangle PAGE_SIZE =
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

    private static final float MARGIN = 36;
    private static final float ROW_HEIGHT = 16;
    private static final float HEADER_FONT_SIZE = 8.5f;
    private static final float BODY_FONT_SIZE = 8;
    private static final float CELL_PADDING = 3;

    private static final DateTimeFormatter CYCLE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter GENERATED_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** One table column: its heading, its width, and how to read it off a batch. */
    private record Column(String heading, float width, Function<BatchDto, String> value) {
    }

    private final List<Column> columns = List.of(
            new Column("Batch Number", 85, b -> text(b.batchNumber())),
            new Column("Cycle Date/Time", 100, b -> b.cycleDateTime() == null ? ""
                    : CYCLE_FORMAT.format(LocalDateTime.ofInstant(b.cycleDateTime(), ZoneId.systemDefault()))),
            new Column("Recipe", 90, b -> text(b.recipeName())),
            new Column("Client", 75, b -> text(b.clientName())),
            new Column("Site", 70, b -> text(b.siteName())),
            new Column("Vehicle", 85, b -> text(b.vehicleNumber())),
            new Column("Driver", 80, b -> text(b.driverName())),
            new Column("Target / Produced", 90, b -> b.targetQuantity() + " / " + b.producedQuantity() + " kg"),
            new Column("Status", 80, b -> b.status() == null ? "" : b.status().name()));

    /**
     * @param filterSummary human-readable description of the filters in force, shown under the title
     * @param note          optional extra line (e.g. a row cap that was hit), or null
     */
    public void write(File file, List<BatchDto> batches, String filterSummary, String note) throws IOException {
        float usableWidth = PAGE_SIZE.getWidth() - (2 * MARGIN);
        float titleBlockHeight = note == null ? 58 : 70;
        float firstRowTop = PAGE_SIZE.getHeight() - MARGIN - titleBlockHeight;
        float bottomLimit = MARGIN + 24;
        int rowsPerPage = Math.max(1, (int) ((firstRowTop - bottomLimit - ROW_HEIGHT) / ROW_HEIGHT));
        int totalPages = Math.max(1, (int) Math.ceil(batches.size() / (double) rowsPerPage));

        try (PDDocument document = new PDDocument()) {
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage page = new PDPage(PAGE_SIZE);
                document.addPage(page);

                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    float y = PAGE_SIZE.getHeight() - MARGIN;

                    drawText(content, boldFont, 14, MARGIN, y - 11, "Batch Reports");
                    drawText(content, bodyFont, 8, MARGIN, y - 26,
                            "Generated " + GENERATED_FORMAT.format(LocalDateTime.now())
                                    + "  ·  " + batches.size() + (batches.size() == 1 ? " batch" : " batches"));
                    drawText(content, bodyFont, 8, MARGIN, y - 38, truncate(filterSummary, bodyFont, 8, usableWidth));
                    if (note != null) {
                        drawText(content, bodyFont, 8, MARGIN, y - 50, truncate(note, bodyFont, 8, usableWidth));
                    }

                    float rowTop = firstRowTop;
                    drawRow(content, rowTop, columns.stream().map(Column::heading).toList(), boldFont,
                            HEADER_FONT_SIZE);
                    // A rule under the headings, so the body does not need a grid.
                    content.setStrokingColor(0.55f, 0.55f, 0.55f);
                    content.setLineWidth(0.7f);
                    content.moveTo(MARGIN, rowTop - ROW_HEIGHT + 3);
                    content.lineTo(MARGIN + tableWidth(), rowTop - ROW_HEIGHT + 3);
                    content.stroke();
                    rowTop -= ROW_HEIGHT;

                    int from = pageIndex * rowsPerPage;
                    int to = Math.min(from + rowsPerPage, batches.size());
                    for (int i = from; i < to; i++) {
                        BatchDto batch = batches.get(i);
                        List<String> cells = new ArrayList<>();
                        for (Column column : columns) {
                            cells.add(column.value().apply(batch));
                        }
                        drawRow(content, rowTop, cells, bodyFont, BODY_FONT_SIZE);
                        rowTop -= ROW_HEIGHT;
                    }

                    String footer = "SmartBatch360  ·  Page " + (pageIndex + 1) + " of " + totalPages;
                    float footerWidth = stringWidth(footer, bodyFont, 8);
                    drawText(content, bodyFont, 8, MARGIN + tableWidth() - footerWidth, MARGIN, footer);
                }
            }

            if (batches.isEmpty()) {
                // Still produce a valid, self-explanatory document rather than a
                // blank page, so an empty export is not mistaken for a failure.
                PDPage onlyPage = document.getPage(0);
                try (PDPageContentStream content = new PDPageContentStream(document, onlyPage,
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    drawText(content, bodyFont, 9, MARGIN, firstRowTop - (2 * ROW_HEIGHT),
                            "No batches matched these filters.");
                }
            }

            document.save(file);
        }
    }

    private float tableWidth() {
        float total = 0;
        for (Column column : columns) {
            total += column.width();
        }
        return total;
    }

    private void drawRow(PDPageContentStream content, float top, List<String> cells, PDFont font, float size)
            throws IOException {
        float x = MARGIN;
        for (int i = 0; i < columns.size(); i++) {
            float width = columns.get(i).width();
            drawText(content, font, size, x + CELL_PADDING, top - 11,
                    truncate(cells.get(i), font, size, width - (2 * CELL_PADDING)));
            x += width;
        }
    }

    private void drawText(PDPageContentStream content, PDFont font, float size, float x, float y, String value)
            throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }

    /** Cut to fit rather than let a long name run into the next column. */
    private String truncate(String value, PDFont font, float size, float maxWidth) {
        String safe = sanitize(value);
        if (stringWidth(safe, font, size) <= maxWidth) {
            return safe;
        }
        String suffix = "...";
        StringBuilder builder = new StringBuilder(safe);
        while (builder.length() > 0
                && stringWidth(builder + suffix, font, size) > maxWidth) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder + suffix;
    }

    private float stringWidth(String value, PDFont font, float size) {
        try {
            return font.getStringWidth(value) / 1000 * size;
        } catch (IOException e) {
            // Width is only needed to decide where to cut; a generous estimate
            // is better than failing the whole export over one label.
            return value.length() * size * 0.6f;
        }
    }

    /**
     * The standard-14 fonts are WinAnsi-encoded and showText throws on anything
     * outside it. A customer name with an unusual character must not be able to
     * fail the export, so replace what cannot be drawn.
     */
    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            builder.append(c == '\n' || c == '\r' || c == '\t' ? ' '
                    : (c >= 32 && c <= 255) ? c : '?');
        }
        return builder.toString();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
