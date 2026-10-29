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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The report for ONE batch, laid out after the sample the user supplied on
 * 28-Oct-2026: a letterhead carrying the title and when the report was made,
 * then General Information, the recipe's target and setpoint for one cycle,
 * what every cycle weighed, and each material's totals against its target,
 * with the whole load's totals in a strip beneath. BatchReportPdfExporter
 * beside this writes the LIST of batches; this is the sheet for one load.
 *
 * Every figure comes from BatchSheet, which the View dialog draws from too.
 *
 * Three things differ from the sample on purpose. The letterhead is the
 * company from Company Details - its logo and name - as this report has always
 * carried, where the sample shows the SmartBatch360 brand. No website is
 * printed, as none is recorded. And "Approved By" is a line to sign rather than
 * a name, since nothing records who approved a batch.
 *
 * Portrait A4 like the sample. A recipe of more than ten materials turns the
 * page to landscape, so its columns stay readable.
 */
public final class BatchSheetPdfExporter {

    private static final PDRectangle PORTRAIT = PDRectangle.A4;
    private static final PDRectangle LANDSCAPE =
            new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final int MOST_MATERIALS_IN_PORTRAIT = 10;

    private static final float MARGIN = 24;
    private static final float FOOTER_SPACE = 30;
    private static final float SECTION_TITLE_HEIGHT = 24;
    private static final float INFO_ROW_HEIGHT = 16.5f;
    private static final float RECIPE_HEADER_HEIGHT = 24;
    private static final float RECIPE_ROW_HEIGHT = 20;
    private static final float CYCLE_GROUP_HEIGHT = 16;
    private static final float CYCLE_NAMES_HEIGHT = 22;
    private static final float CYCLE_ROW_HEIGHT = 15;
    private static final float CYCLE_TOTAL_HEIGHT = 17;
    private static final float SUMMARY_HEADER_HEIGHT = 20;
    private static final float SUMMARY_ROW_HEIGHT = 15;
    private static final float TOTALS_HEIGHT = 52;
    private static final float REMARKS_HEIGHT = 46;
    private static final float GAP = 12;
    private static final String TITLE = "BATCH REPORT";
    private static final float TITLE_SIZE = 20;

    private static final Color NAVY = new Color(27, 47, 78);
    private static final Color TEXT = new Color(28, 36, 48);
    private static final Color MUTED = new Color(107, 118, 134);
    private static final Color HEAD_FILL = new Color(233, 237, 243);
    private static final Color BORDER = new Color(199, 206, 216);
    private static final Color GREEN = new Color(30, 142, 62);
    private static final Color RED = new Color(217, 48, 37);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
    private static final DateTimeFormatter CYCLE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private enum Align { LEFT, CENTER }

    /**
     * @param company the company row to print as the letterhead, or null to leave it as SmartBatch360
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
        BatchSheet sheet = BatchSheet.of(batch, cycles);
        PDRectangle size = sheet.materialCount() > MOST_MATERIALS_IN_PORTRAIT ? LANDSCAPE : PORTRAIT;
        PDDocument document = new PDDocument();
        try {
            Layout layout = new Layout(document, size, batch);
            try {
                layout.letterhead(company, logo);
                layout.generalInformation(batch, sheet);
                layout.recipeTargets(sheet);
                layout.cycleDetails(sheet);
                layout.batchSummary(sheet);
                layout.totalsAndRemarks(sheet);
            } finally {
                layout.close();
            }
            drawFooters(document, batch);
            return document;
        } catch (IOException | RuntimeException e) {
            // Handed back open on success, so on failure it is ours to close.
            document.close();
            throw e;
        }
    }

    /**
     * Writes down the pages top to bottom, starting a new page whenever the
     * next piece will not fit above the footer.
     */
    private final class Layout {

        private final PDDocument document;
        private final PDRectangle size;
        private final BatchDto batch;
        private final float left = MARGIN;
        private final float width;
        private PDPageContentStream content;
        private float y;

        private Layout(PDDocument document, PDRectangle size, BatchDto batch) throws IOException {
            this.document = document;
            this.size = size;
            this.batch = batch;
            this.width = size.getWidth() - (2 * MARGIN);
            newPage(true);
        }

        private void newPage(boolean first) throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(size);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = size.getHeight() - MARGIN;
            if (!first) {
                continuationHeader();
            }
        }

        /** @return whether a new page had to be started to make the room */
        private boolean ensure(float height) throws IOException {
            if (y - height < MARGIN + FOOTER_SPACE) {
                newPage(false);
                return true;
            }
            return false;
        }

        private void close() throws IOException {
            content.close();
        }

        // ---------------------------------------------------------- letterhead

        private void letterhead(HeaderDto company, HeaderLogoDto logo) throws IOException {
            float top = y;
            float pageWidth = size.getWidth();

            // Left: the company's logo and name, as the report has always carried.
            byte[] logoBytes = logo != null && logo.data() != null && logo.data().length > 0 ? logo.data() : appIcon();
            drawImage(logoBytes, left, top - 52, 50, 50);
            String name = company == null || isBlank(company.companyName()) ? "SmartBatch360" : company.companyName();
            float textLeft = left + 58;
            // Up to where the centred title begins. A long name is set smaller
            // before it is ever cut short.
            float titleLeft = (pageWidth / 2) - (stringWidth(TITLE, boldFont, TITLE_SIZE) / 2);
            float textWidth = titleLeft - 12 - textLeft;
            float nameSize = Math.max(9, fitSize(name, boldFont, 13, textWidth));
            text(boldFont, nameSize, NAVY, textLeft, top - 20, truncate(name, boldFont, nameSize, textWidth));
            float lineY = top - 31;
            for (String line : letterheadLines(company)) {
                text(bodyFont, 7.5f, MUTED, textLeft, lineY, truncate(line, bodyFont, 7.5f, textWidth));
                lineY -= 9;
            }

            // Centre: what this is.
            centred(boldFont, TITLE_SIZE, NAVY, pageWidth / 2, top - 24, TITLE);
            centred(bodyFont, 9.5f, NAVY, pageWidth / 2, top - 39, "PRODUCTION BATCH SUMMARY");

            // Right: when it was made.
            float boxWidth = 138;
            float boxX = pageWidth - MARGIN - boxWidth;
            float boxBottom = top - 46;
            content.setStrokingColor(NAVY);
            content.setLineWidth(0.8f);
            roundRect(content, boxX, boxBottom, boxWidth, 42, 4);
            content.stroke();
            calendarIcon(boxX + 10, top - 9);
            text(bodyFont, 8, TEXT, boxX + 28, top - 18, "Report Generated");
            LocalDateTime now = LocalDateTime.now();
            String generated = DATE.format(now) + "  " + TIME.format(now);
            text(boldFont, fitSize(generated, boldFont, 9, boxWidth - 20), TEXT, boxX + 10, top - 35, generated);

            y = top - 60;
            line(NAVY, 1.2f, left, y, left + width, y);
            y -= GAP;
        }

        private List<String> letterheadLines(HeaderDto company) {
            List<String> lines = new ArrayList<>();
            if (company == null) {
                return lines;
            }
            if (!isBlank(company.plantName())) {
                lines.add(company.plantName());
            }
            String address = joinNonBlank(", ", company.address(), joinNonBlank(" - ", company.city(),
                    company.pinCode()));
            if (!address.isBlank()) {
                lines.add(address);
            }
            if (!isBlank(company.gstin())) {
                lines.add("GSTIN: " + company.gstin());
            }
            return lines.size() > 3 ? lines.subList(0, 3) : lines;
        }

        /**
         * Scaled to fit its box, never stretched. An image that cannot be
         * decoded is left out rather than failing the report - the batch's
         * figures are what the sheet is for.
         */
        private void drawImage(byte[] data, float x, float bottom, float maxWidth, float maxHeight) {
            if (data == null || data.length == 0) {
                return;
            }
            try {
                PDImageXObject image = PDImageXObject.createFromByteArray(document, data, "logo");
                float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
                float w = image.getWidth() * scale;
                float h = image.getHeight() * scale;
                content.drawImage(image, x + ((maxWidth - w) / 2), bottom + ((maxHeight - h) / 2), w, h);
            } catch (IOException | RuntimeException e) {
                // Deliberately nothing: see the method comment.
            }
        }

        private void continuationHeader() throws IOException {
            float top = y;
            text(boldFont, 12, NAVY, left, top - 12, "BATCH REPORT");
            String note = "Batch " + orBlank(batch.batchNumber()) + " (continued)";
            text(bodyFont, 8.5f, MUTED, left + width - stringWidth(note, bodyFont, 8.5f), top - 11, note);
            y = top - 20;
            line(NAVY, 0.8f, left, y, left + width, y);
            y -= GAP;
        }

        // -------------------------------------------------- general information

        private void generalInformation(BatchDto batch, BatchSheet sheet) throws IOException {
            String[][] leftRows = {
                    {"Batch Number", orBlank(batch.batchNumber())},
                    {"Customer", orBlank(batch.clientName())},
                    {"Site Name", orBlank(batch.siteName())},
                    {"Vehicle Number", orBlank(batch.vehicleNumber())},
                    {"Driver Name", orBlank(batch.driverName())},
                    {"Order Number", batch.orderId() == null ? "-" : "#" + batch.orderId()}};
            String[][] rightRows = {
                    {"Batch Date", batch.cycleDateTime() == null ? "-" : DATE.format(local(batch.cycleDateTime()))},
                    {"Batch Start Time", batch.cycleDateTime() == null ? "-" : TIME.format(local(batch.cycleDateTime()))},
                    {"Batch End Time", endTime(batch, sheet)},
                    {"Recipe Name", orBlank(batch.recipeName())},
                    {"Batch Quantity", batchQuantity(batch)},
                    {"Shift", isBlank(batch.shift()) ? "-" : batch.shift()}};

            float height = leftRows.length * INFO_ROW_HEIGHT;
            sectionTitle("1. GENERAL INFORMATION", null, height);
            float half = width / 2;
            infoColumn(leftRows, left + 16, half - 32);
            infoColumn(rightRows, left + half + 16, half - 32);
            line(BORDER, 0.6f, left + half, y + 2, left + half, y - height + 4);
            y -= height + GAP;
        }

        private void infoColumn(String[][] rows, float x, float columnWidth) throws IOException {
            float rowY = y - 11;
            for (String[] row : rows) {
                text(bodyFont, 9, TEXT, x, rowY, row[0]);
                text(bodyFont, 9, TEXT, x + 92, rowY, ":");
                text(bodyFont, 9, TEXT, x + 106, rowY, truncate(row[1], bodyFont, 9, columnWidth - 106));
                rowY -= INFO_ROW_HEIGHT;
            }
        }

        // ------------------------------------------------------ recipe targets

        private void recipeTargets(BatchSheet sheet) throws IOException {
            List<BatchSheet.MaterialFigures> materials = sheet.materials();
            float height = RECIPE_HEADER_HEIGHT + (2 * RECIPE_ROW_HEIGHT);
            sectionTitle("2. RECIPE / TARGET", "(Per Cycle)", height);

            float labelWidth = 74;
            float each = (width - labelWidth) / (materials.size() + 1);
            float size = cellFontSize(each);

            float x = left;
            float top = y;
            cell(x, top, labelWidth, RECIPE_HEADER_HEIGHT, List.of("Material"), boldFont, size, HEAD_FILL, TEXT,
                    Align.CENTER);
            x += labelWidth;
            for (BatchSheet.MaterialFigures material : materials) {
                List<String> lines = headingLines(material.name(), each, size);
                cell(x, top, each, RECIPE_HEADER_HEIGHT, lines, boldFont, headingSize(lines, each, size), HEAD_FILL,
                        TEXT, Align.CENTER);
                x += each;
            }
            cell(x, top, each, RECIPE_HEADER_HEIGHT, List.of("TOTAL"), boldFont, size, HEAD_FILL, TEXT, Align.CENTER);

            BigDecimal targetTotal = null;
            BigDecimal setpointTotal = null;
            for (BatchSheet.MaterialFigures material : materials) {
                targetTotal = add(targetTotal, material.perCycleTarget());
                setpointTotal = add(setpointTotal, material.perCycleSetpoint());
            }
            recipeRow(top - RECIPE_HEADER_HEIGHT, "Target (kg)", labelWidth, each, size, materials, true, targetTotal);
            recipeRow(top - RECIPE_HEADER_HEIGHT - RECIPE_ROW_HEIGHT, "Setpoint (kg)", labelWidth, each, size,
                    materials, false, setpointTotal);
            y = top - height - GAP;
        }

        private void recipeRow(float top, String label, float labelWidth, float each, float size,
                               List<BatchSheet.MaterialFigures> materials, boolean target, BigDecimal total)
                throws IOException {
            float x = left;
            cell(x, top, labelWidth, RECIPE_ROW_HEIGHT, List.of(label), bodyFont, size, null, TEXT, Align.CENTER);
            x += labelWidth;
            for (BatchSheet.MaterialFigures material : materials) {
                BigDecimal value = target ? material.perCycleTarget() : material.perCycleSetpoint();
                cell(x, top, each, RECIPE_ROW_HEIGHT, List.of(kg(value)), bodyFont, size, null, TEXT, Align.CENTER);
                x += each;
            }
            cell(x, top, each, RECIPE_ROW_HEIGHT, List.of(kg(total)), boldFont, size, null, TEXT, Align.CENTER);
        }

        // ------------------------------------------------------- cycle details

        private void cycleDetails(BatchSheet sheet) throws IOException {
            List<BatchSheet.MaterialFigures> materials = sheet.materials();
            List<BatchSheet.CycleFigures> cycles = sheet.cycles();
            float numberWidth = 46;
            float timeWidth = 54;
            float totalWidth = 60;
            float each = (width - numberWidth - timeWidth - totalWidth) / Math.max(1, materials.size());
            float[] widths = new float[materials.size() + 3];
            widths[0] = numberWidth;
            widths[1] = timeWidth;
            for (int i = 0; i < materials.size(); i++) {
                widths[2 + i] = each;
            }
            widths[widths.length - 1] = totalWidth;
            float size = cellFontSize(each);

            float headerHeight = CYCLE_GROUP_HEIGHT + CYCLE_NAMES_HEIGHT;
            // The title, the header and at least the first line - or the last
            // one and its total - stay together.
            float firstPiece = headerHeight + CYCLE_ROW_HEIGHT + (cycles.size() <= 1 ? CYCLE_TOTAL_HEIGHT : 0);
            sectionTitle("3. CYCLE DETAILS", "(Achieved Values per Cycle)", firstPiece);
            cycleHeader(materials, widths, size);

            if (cycles.isEmpty()) {
                cell(left, y, width, CYCLE_ROW_HEIGHT, List.of("No cycles have been recorded for this batch yet."),
                        bodyFont, size, null, MUTED, Align.CENTER);
                y -= CYCLE_ROW_HEIGHT + GAP;
                return;
            }

            for (int i = 0; i < cycles.size(); i++) {
                boolean last = i == cycles.size() - 1;
                if (ensure(CYCLE_ROW_HEIGHT + (last ? CYCLE_TOTAL_HEIGHT : 0))) {
                    sectionTitle("3. CYCLE DETAILS", "(continued)", 0);
                    cycleHeader(materials, widths, size);
                }
                BatchSheet.CycleFigures cycle = cycles.get(i);
                List<String> cells = new ArrayList<>();
                cells.add(cycle.number() == null ? "" : String.valueOf(cycle.number()));
                cells.add(cycle.time() == null ? "" : CYCLE_TIME.format(local(cycle.time())));
                cycle.weighed().forEach(value -> cells.add(value == null ? "" : kg(value)));
                cells.add(kg(cycle.total()));
                row(cells, widths, CYCLE_ROW_HEIGHT, bodyFont, size, null);
            }

            // What the whole batch weighed, material by material.
            List<String> totals = new ArrayList<>();
            BigDecimal grand = BigDecimal.ZERO;
            for (BatchSheet.MaterialFigures material : materials) {
                totals.add(kg(material.achievedTotal()));
                grand = grand.add(material.achievedTotal());
            }
            float x = left;
            cell(x, y, widths[0] + widths[1], CYCLE_TOTAL_HEIGHT, List.of("TOTAL (Batch)"), boldFont, size, HEAD_FILL,
                    NAVY, Align.CENTER);
            x += widths[0] + widths[1];
            for (int i = 0; i < totals.size(); i++) {
                cell(x, y, widths[2 + i], CYCLE_TOTAL_HEIGHT, List.of(totals.get(i)), boldFont, size, HEAD_FILL, NAVY,
                        Align.CENTER);
                x += widths[2 + i];
            }
            cell(x, y, widths[widths.length - 1], CYCLE_TOTAL_HEIGHT, List.of(kg(grand)), boldFont, size, HEAD_FILL,
                    NAVY, Align.CENTER);
            y -= CYCLE_TOTAL_HEIGHT + GAP;
        }

        /** Cycle No. and Start Time across both rows, the materials under "ACHIEVED WEIGHT (kg)", then TOTAL. */
        private void cycleHeader(List<BatchSheet.MaterialFigures> materials, float[] widths, float size)
                throws IOException {
            float headerHeight = CYCLE_GROUP_HEIGHT + CYCLE_NAMES_HEIGHT;
            float top = y;
            float x = left;
            cell(x, top, widths[0], headerHeight, List.of("Cycle No."), boldFont, size, HEAD_FILL, TEXT, Align.CENTER);
            x += widths[0];
            cell(x, top, widths[1], headerHeight, List.of("Start Time"), boldFont, size, HEAD_FILL, TEXT,
                    Align.CENTER);
            x += widths[1];
            float groupWidth = 0;
            for (int i = 0; i < materials.size(); i++) {
                groupWidth += widths[2 + i];
            }
            cell(x, top, groupWidth, CYCLE_GROUP_HEIGHT, List.of("ACHIEVED WEIGHT (kg)"), boldFont, size, HEAD_FILL,
                    TEXT, Align.CENTER);
            for (int i = 0; i < materials.size(); i++) {
                List<String> lines = headingLines(materials.get(i).name(), widths[2 + i], size);
                cell(x, top - CYCLE_GROUP_HEIGHT, widths[2 + i], CYCLE_NAMES_HEIGHT, lines, boldFont,
                        headingSize(lines, widths[2 + i], size), HEAD_FILL, TEXT, Align.CENTER);
                x += widths[2 + i];
            }
            cell(x, top, widths[widths.length - 1], headerHeight, List.of("TOTAL (kg)"), boldFont, size, HEAD_FILL,
                    TEXT, Align.CENTER);
            y = top - headerHeight;
        }

        // ------------------------------------------------------- batch summary

        private void batchSummary(BatchSheet sheet) throws IOException {
            List<BatchSheet.MaterialFigures> materials = sheet.materials();
            float height = SUMMARY_HEADER_HEIGHT + (materials.size() * SUMMARY_ROW_HEIGHT);
            // The summary, the totals strip and the remarks stay on one page,
            // so the load's totals can be read against its materials.
            sectionTitle("4. BATCH SUMMARY", "(Totals)",
                    height + GAP + TOTALS_HEIGHT + GAP + REMARKS_HEIGHT);

            float nameWidth = width * 0.24f;
            float each = (width - nameWidth) / 5;
            float[] widths = {nameWidth, each, each, each, each, each};
            String[] headings = {"Material", "Target Total (kg)", "Setpoint Total (kg)", "Achieved Total (kg)",
                    "Difference (kg)", "Difference (%)"};
            float x = left;
            for (int i = 0; i < headings.length; i++) {
                cell(x, y, widths[i], SUMMARY_HEADER_HEIGHT, List.of(headings[i]), boldFont, 8, HEAD_FILL, TEXT,
                        Align.CENTER);
                x += widths[i];
            }
            y -= SUMMARY_HEADER_HEIGHT;

            for (BatchSheet.MaterialFigures material : materials) {
                x = left;
                cell(x, y, widths[0], SUMMARY_ROW_HEIGHT, List.of(material.name()), bodyFont, 8, null, TEXT,
                        Align.LEFT);
                x += widths[0];
                cell(x, y, widths[1], SUMMARY_ROW_HEIGHT, List.of(kg(material.targetTotal())), bodyFont, 8, null,
                        TEXT, Align.CENTER);
                x += widths[1];
                cell(x, y, widths[2], SUMMARY_ROW_HEIGHT, List.of(kg(material.setpointTotal())), bodyFont, 8, null,
                        TEXT, Align.CENTER);
                x += widths[2];
                cell(x, y, widths[3], SUMMARY_ROW_HEIGHT, List.of(kg(material.achievedTotal())), bodyFont, 8, null,
                        TEXT, Align.CENTER);
                x += widths[3];
                cell(x, y, widths[4], SUMMARY_ROW_HEIGHT, List.of(signedKg(material.difference())), bodyFont, 8, null,
                        signColour(material.difference()), Align.CENTER);
                x += widths[4];
                cell(x, y, widths[5], SUMMARY_ROW_HEIGHT, List.of(signedPercent(material.differencePercent())),
                        bodyFont, 8, null, signColour(material.differencePercent()), Align.CENTER);
                y -= SUMMARY_ROW_HEIGHT;
            }
            y -= GAP;
        }

        // ------------------------------------------------- totals and remarks

        private void totalsAndRemarks(BatchSheet sheet) throws IOException {
            ensure(TOTALS_HEIGHT + GAP + REMARKS_HEIGHT);

            BigDecimal target = null;
            BigDecimal setpoint = BigDecimal.ZERO;
            BigDecimal achieved = BigDecimal.ZERO;
            for (BatchSheet.MaterialFigures material : sheet.materials()) {
                target = add(target, material.targetTotal());
                setpoint = setpoint.add(material.setpointTotal());
                achieved = achieved.add(material.achievedTotal());
            }
            BigDecimal difference = target == null ? null : achieved.subtract(target);
            BigDecimal percent = difference == null || target.signum() == 0 ? null
                    : difference.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP);

            float top = y;
            content.setStrokingColor(BORDER);
            content.setLineWidth(0.8f);
            roundRect(content, left, top - TOTALS_HEIGHT, width, TOTALS_HEIGHT, 4);
            content.stroke();
            float each = width / 5;
            String[] labels = {"TARGET TOTAL (kg)", "SETPOINT TOTAL (kg)", "ACHIEVED TOTAL (kg)",
                    "TOTAL DIFFERENCE (kg)", "TOTAL DIFFERENCE (%)"};
            String[] values = {kg(target), kg(setpoint), kg(achieved), signedKg(difference), signedPercent(percent)};
            Color[] colours = {NAVY, NAVY, GREEN, signColour(difference), signColour(percent)};
            // One size for all five labels, set by the longest, so the strip reads evenly.
            float labelSize = 6.5f;
            for (String label : labels) {
                labelSize = Math.min(labelSize, fitSize(label, boldFont, 6.5f, each - 38));
            }
            for (int i = 0; i < 5; i++) {
                float x = left + (i * each);
                if (i > 0) {
                    line(BORDER, 0.6f, x, top - 9, x, top - TOTALS_HEIGHT + 9);
                }
                float iconBottom = top - (TOTALS_HEIGHT / 2) - 9;
                switch (i) {
                    case 0 -> targetIcon(x + 18, iconBottom + 9);
                    case 1 -> slidersIcon(x + 9, iconBottom);
                    case 2 -> weightIcon(x + 9, iconBottom);
                    case 3 -> chartIcon(x + 9, iconBottom);
                    default -> text(boldFont, 20, NAVY, x + 9, iconBottom + 2, "%");
                }
                float textX = x + 34;
                float textWidth = each - 38;
                text(boldFont, labelSize, TEXT, textX, top - 19, labels[i]);
                text(boldFont, fitSize(values[i], boldFont, 13, textWidth), colours[i], textX, top - 38, values[i]);
            }
            y = top - TOTALS_HEIGHT - GAP;

            top = y;
            content.setStrokingColor(BORDER);
            roundRect(content, left, top - REMARKS_HEIGHT, width, REMARKS_HEIGHT, 4);
            content.stroke();
            speechIcon(left + 12, top - 32);
            text(boldFont, 10, NAVY, left + 42, top - 18, "REMARKS");
            text(bodyFont, 9, TEXT, left + 42, top - 34, "-");
            float signX = left + (width * 0.62f);
            line(BORDER, 0.6f, signX, top - 8, signX, top - REMARKS_HEIGHT + 8);
            text(boldFont, 8.5f, TEXT, signX + 16, top - 19, "Prepared By");
            text(boldFont, 8.5f, TEXT, signX + 86, top - 19, ":");
            text(bodyFont, 8.5f, TEXT, signX + 98, top - 19, "System");
            text(boldFont, 8.5f, TEXT, signX + 16, top - 35, "Approved By");
            text(boldFont, 8.5f, TEXT, signX + 86, top - 35, ":");
            line(MUTED, 0.5f, signX + 98, top - 37, left + width - 16, top - 37);
            y = top - REMARKS_HEIGHT;
        }

        // ------------------------------------------------------------ pieces

        /**
         * A section's icon and title, kept on the same page as the first
         * {@code followedBy} points of what comes under it.
         */
        private void sectionTitle(String title, String subtitle, float followedBy) throws IOException {
            ensure(SECTION_TITLE_HEIGHT + followedBy);
            float top = y;
            sectionIcon(left, top);
            text(boldFont, 11.5f, NAVY, left + 23, top - 11.5f, title);
            if (subtitle != null) {
                text(bodyFont, 8.5f, TEXT, left + 23 + stringWidth(title, boldFont, 11.5f) + 5, top - 11.5f, subtitle);
            }
            y = top - SECTION_TITLE_HEIGHT;
        }

        private void row(List<String> cells, float[] widths, float height, PDFont font, float size, Color fill)
                throws IOException {
            float x = left;
            for (int i = 0; i < widths.length; i++) {
                cell(x, y, widths[i], height, List.of(cells.get(i)), font, size, fill, TEXT, Align.CENTER);
                x += widths[i];
            }
            y -= height;
        }

        /** A bordered cell whose top edge is at {@code top}, its lines centred within it. */
        private void cell(float x, float top, float w, float h, List<String> lines, PDFont font, float size,
                          Color fill, Color colour, Align align) throws IOException {
            if (fill != null) {
                content.setNonStrokingColor(fill);
                content.addRect(x, top - h, w, h);
                content.fill();
            }
            content.setStrokingColor(BORDER);
            content.setLineWidth(0.6f);
            content.addRect(x, top - h, w, h);
            content.stroke();

            float lineHeight = size + 1.5f;
            float firstBaseline = top - (h / 2) + ((lines.size() - 1) * lineHeight / 2) - (size * 0.35f);
            for (int i = 0; i < lines.size(); i++) {
                String value = truncate(lines.get(i), font, size, w - 6);
                float baseline = firstBaseline - (i * lineHeight);
                float textX = align == Align.LEFT ? x + 6 : x + ((w - stringWidth(value, font, size)) / 2);
                text(font, size, colour, textX, baseline, value);
            }
        }

        private void text(PDFont font, float size, Color colour, float x, float baseline, String value)
                throws IOException {
            content.setNonStrokingColor(colour);
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, baseline);
            content.showText(sanitize(value));
            content.endText();
        }

        private void centred(PDFont font, float size, Color colour, float centreX, float baseline, String value)
                throws IOException {
            text(font, size, colour, centreX - (stringWidth(value, font, size) / 2), baseline, value);
        }

        private void line(Color colour, float lineWidth, float x1, float y1, float x2, float y2) throws IOException {
            content.setStrokingColor(colour);
            content.setLineWidth(lineWidth);
            content.moveTo(x1, y1);
            content.lineTo(x2, y2);
            content.stroke();
        }

        // ------------------------------------------------------------- icons

        /** A navy tile with three lines on it, like a page of a report. */
        private void sectionIcon(float x, float top) throws IOException {
            content.setNonStrokingColor(NAVY);
            roundRect(content, x, top - 16, 16, 16, 3);
            content.fill();
            content.setNonStrokingColor(Color.WHITE);
            content.addRect(x + 4, top - 5.5f, 8, 1.5f);
            content.addRect(x + 4, top - 8.75f, 8, 1.5f);
            content.addRect(x + 4, top - 12, 5, 1.5f);
            content.fill();
        }

        private void calendarIcon(float x, float top) throws IOException {
            content.setStrokingColor(NAVY);
            content.setLineWidth(1);
            roundRect(content, x, top - 12, 12, 11, 1.5f);
            content.stroke();
            content.setNonStrokingColor(NAVY);
            content.addRect(x, top - 4, 12, 3);
            content.fill();
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 3; column++) {
                    content.addRect(x + 2 + (column * 3), top - 7.5f - (row * 2.8f), 1.6f, 1.6f);
                }
            }
            content.fill();
            line(NAVY, 1.2f, x + 3.5f, top - 1.5f, x + 3.5f, top + 1);
            line(NAVY, 1.2f, x + 8.5f, top - 1.5f, x + 8.5f, top + 1);
        }

        private void targetIcon(float cx, float cy) throws IOException {
            content.setStrokingColor(NAVY);
            content.setLineWidth(1.6f);
            circle(content, cx, cy, 9);
            content.stroke();
            circle(content, cx, cy, 5);
            content.stroke();
            content.setNonStrokingColor(NAVY);
            circle(content, cx, cy, 1.8f);
            content.fill();
        }

        private void slidersIcon(float x, float bottom) throws IOException {
            float[] knobs = {11, 4, 8};
            for (int i = 0; i < 3; i++) {
                float lineX = x + 3 + (i * 6);
                line(NAVY, 1.4f, lineX, bottom, lineX, bottom + 18);
                content.setNonStrokingColor(NAVY);
                content.addRect(lineX - 2.5f, bottom + knobs[i], 5, 3.5f);
                content.fill();
            }
        }

        /** A weight with its handle - what the scales read. */
        private void weightIcon(float x, float bottom) throws IOException {
            content.setNonStrokingColor(NAVY);
            roundRect(content, x, bottom, 18, 12.5f, 3);
            content.fill();
            content.setStrokingColor(NAVY);
            content.setLineWidth(1.6f);
            roundRect(content, x + 5, bottom + 11, 8, 6.5f, 3);
            content.stroke();
            text(boldFont, 5.5f, Color.WHITE, x + 9 - (stringWidth("kg", boldFont, 5.5f) / 2), bottom + 4.5f, "kg");
        }

        private void chartIcon(float x, float bottom) throws IOException {
            line(NAVY, 1.2f, x, bottom + 18, x, bottom);
            line(NAVY, 1.2f, x, bottom, x + 18, bottom);
            content.setNonStrokingColor(NAVY);
            content.addRect(x + 3, bottom + 1.5f, 3.2f, 6);
            content.addRect(x + 8, bottom + 1.5f, 3.2f, 10);
            content.addRect(x + 13, bottom + 1.5f, 3.2f, 14);
            content.fill();
        }

        private void speechIcon(float x, float bottom) throws IOException {
            content.setNonStrokingColor(NAVY);
            roundRect(content, x, bottom + 4, 20, 14, 4);
            content.fill();
            content.moveTo(x + 4, bottom + 5);
            content.lineTo(x + 4, bottom);
            content.lineTo(x + 9, bottom + 5);
            content.closePath();
            content.fill();
            content.setNonStrokingColor(Color.WHITE);
            content.addRect(x + 4.5f, bottom + 12.5f, 11, 1.4f);
            content.addRect(x + 4.5f, bottom + 9, 7, 1.4f);
            content.fill();
        }
    }

    // ---------------------------------------------------------------- footers

    private void drawFooters(PDDocument document, BatchDto batch) throws IOException {
        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = document.getPage(i);
            float pageWidth = page.getMediaBox().getWidth();
            try (PDPageContentStream content = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true)) {
                content.setStrokingColor(BORDER);
                content.setLineWidth(0.6f);
                content.moveTo(MARGIN, MARGIN + 12);
                content.lineTo(pageWidth - MARGIN, MARGIN + 12);
                content.stroke();
                footerText(content, MARGIN, "SmartBatch360 Control System");
                String middle = "Batch " + orBlank(batch.batchNumber());
                footerText(content, (pageWidth - stringWidth(middle, bodyFont, 7.5f)) / 2, middle);
                String pageNumber = "Page " + (i + 1) + " of " + total;
                footerText(content, pageWidth - MARGIN - stringWidth(pageNumber, bodyFont, 7.5f), pageNumber);
            }
        }
    }

    private void footerText(PDPageContentStream content, float x, String value) throws IOException {
        content.setNonStrokingColor(MUTED);
        content.beginText();
        content.setFont(bodyFont, 7.5f);
        content.newLineAtOffset(x, MARGIN);
        content.showText(sanitize(value));
        content.endText();
    }

    // ----------------------------------------------------------------- helpers

    /** When the batch's last cycle ran - known only once it has stopped running. */
    private String endTime(BatchDto batch, BatchSheet sheet) {
        Instant last = sheet.cycles().stream().map(BatchSheet.CycleFigures::time).filter(Objects::nonNull)
                .max(Instant::compareTo).orElse(null);
        if (last == null) {
            return "-";
        }
        boolean running = batch.status() == BatchStatus.PENDING || batch.status() == BatchStatus.IN_PROGRESS
                || batch.status() == BatchStatus.PAUSED;
        return running ? "In progress" : TIME.format(local(last));
    }

    private String batchQuantity(BatchDto batch) {
        if (batch.batchSizeM3() == null) {
            return "-";
        }
        String size = plain(batch.batchSizeM3()) + " m³";
        if (batch.perCycleM3() == null || batch.cycleNumber() == null) {
            return size;
        }
        return size + "  (" + batch.cycleNumber() + " cycles of " + plain(batch.perCycleM3()) + " m³)";
    }

    /** SmartBatch360's own icon, standing in when no company logo has been uploaded. */
    private byte[] appIcon() {
        try (InputStream in = BatchSheetPdfExporter.class.getResourceAsStream("/images/app-icon.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /** One line, or two when a material's name will not fit across its column. */
    private List<String> headingLines(String name, float width, float size) {
        String safe = sanitize(name);
        if (stringWidth(safe, boldFont, size) <= width - 6) {
            return List.of(safe);
        }
        int split = safe.lastIndexOf(' ');
        while (split > 0 && stringWidth(safe.substring(0, split), boldFont, size) > width - 6) {
            split = safe.lastIndexOf(' ', split - 1);
        }
        if (split <= 0) {
            // Even the first word is too wide - break there anyway, so a narrow
            // column reads "Crushed / Sand" rather than "Crush..".
            split = safe.indexOf(' ');
        }
        if (split <= 0) {
            return List.of(safe);
        }
        return List.of(safe.substring(0, split), safe.substring(split + 1));
    }

    /**
     * A material's heading a little smaller where one of its words is wider
     * than a narrow column - never below 5.5 pt, past which it is cut instead.
     */
    private float headingSize(List<String> lines, float width, float size) {
        float fitted = size;
        for (String line : lines) {
            fitted = Math.min(fitted, fitSize(line, boldFont, size, width - 6));
        }
        return Math.max(5.5f, fitted);
    }

    private float cellFontSize(float columnWidth) {
        return columnWidth >= 55 ? 8.5f : columnWidth >= 42 ? 7.5f : 6.5f;
    }

    /** The largest size up to {@code preferred} at which the text fits. */
    private float fitSize(String value, PDFont font, float preferred, float maxWidth) {
        float size = preferred;
        while (size > 5 && stringWidth(value, font, size) > maxWidth) {
            size -= 0.5f;
        }
        return size;
    }

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
            return font.getStringWidth(sanitize(value)) / 1000 * size;
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

    private static void roundRect(PDPageContentStream content, float x, float y, float w, float h, float r)
            throws IOException {
        float k = 0.5523f * r;
        content.moveTo(x + r, y);
        content.lineTo(x + w - r, y);
        content.curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
        content.lineTo(x + w, y + h - r);
        content.curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
        content.lineTo(x + r, y + h);
        content.curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
        content.lineTo(x, y + r);
        content.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        content.closePath();
    }

    private static void circle(PDPageContentStream content, float cx, float cy, float r) throws IOException {
        float k = 0.5523f * r;
        content.moveTo(cx + r, cy);
        content.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
        content.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
        content.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);
        content.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);
        content.closePath();
    }

    private static LocalDateTime local(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static DecimalFormat kgFormat() {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    }

    private static String kg(BigDecimal value) {
        return value == null ? "-" : kgFormat().format(value.setScale(2, RoundingMode.HALF_UP));
    }

    private static String signedKg(BigDecimal value) {
        return value == null ? "-" : (value.signum() > 0 ? "+" : "") + kg(value);
    }

    private static String signedPercent(BigDecimal value) {
        return value == null ? "-" : (value.signum() > 0 ? "+" : "") + kg(value) + "%";
    }

    /** Over the target in green and under it in red, as the sample shows them. */
    private static Color signColour(BigDecimal value) {
        if (value == null || value.signum() == 0) {
            return TEXT;
        }
        return value.signum() > 0 ? GREEN : RED;
    }

    private static BigDecimal add(BigDecimal total, BigDecimal value) {
        if (value == null) {
            return total;
        }
        return total == null ? value : total.add(value);
    }

    private static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String orBlank(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String joinNonBlank(String separator, String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (!isBlank(part)) {
                kept.add(part.trim());
            }
        }
        return String.join(separator, kept);
    }
}
