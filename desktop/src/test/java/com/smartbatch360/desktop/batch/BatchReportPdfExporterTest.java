package com.smartbatch360.desktop.batch;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The PDF export, which is drawn by hand on PDFBox - so the things worth
 * pinning down are the ones hand-drawn layout gets wrong: pagination, a name
 * too long for its column, and a character the font cannot encode (showText
 * throws on those, which would fail the whole export over one customer name).
 *
 * A sample is left in target/test-output so the layout can be looked at rather
 * than only asserted about.
 */
class BatchReportPdfExporterTest {

    private final BatchReportPdfExporter exporter = new BatchReportPdfExporter();

    private BatchDto batch(int index, String clientName) {
        return new BatchDto(
                (long) index,
                "250" + String.format("%03d", index),
                2L, "M20 Concrete",
                null,
                1L, clientName,
                1L, "Kharadi",
                1L, "MH12PQ3457",
                1L, "Ganesh More",
                new BigDecimal("310.00"), new BigDecimal("308.50"), new BigDecimal("1.50"),
                Instant.parse("2026-09-12T06:30:00Z"),
                index, "Day",
                BatchStatus.COMPLETED,
                EquipmentStatus.STOPPED, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                List.of(),
                Instant.parse("2026-09-12T06:30:00Z"), Instant.parse("2026-09-12T06:30:00Z"));
    }

    private List<BatchDto> batches(int count) {
        List<BatchDto> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(batch(i, "Suraj"));
        }
        return rows;
    }

    private File outputFile(String name) throws IOException {
        Path dir = Path.of("target", "test-output");
        Files.createDirectories(dir);
        return dir.resolve(name).toFile();
    }

    @Test
    void writesTheRowsItIsGiven() throws IOException {
        File file = outputFile("batch-reports-single-page.pdf");

        exporter.write(file, batches(8), "Filters: Client Suraj", null);

        assertThat(file).exists();
        try (PDDocument document = Loader.loadPDF(file)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Batch Reports", "Filters: Client Suraj", "250001", "250008");
            assertThat(text).contains("8 batches");
            assertThat(text).contains("Page 1 of 1");
        }
    }

    /** More rows than fit on a page must carry on, with headings repeated. */
    @Test
    void paginatesAndRepeatsTheHeadings() throws IOException {
        File file = outputFile("batch-reports-paginated.pdf");

        exporter.write(file, batches(60), "All batches (no filters applied)", null);

        try (PDDocument document = Loader.loadPDF(file)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String secondPage = stripper.getText(document);
            assertThat(secondPage).contains("Batch Number", "Status");
            assertThat(secondPage).contains("Page 2 of " + document.getNumberOfPages());
        }
    }

    /**
     * A name longer than its column is cut, not spilled into the neighbour -
     * and the untruncated form must not survive in the page text.
     */
    @Test
    void cutsAValueTooLongForItsColumn() throws IOException {
        File file = outputFile("batch-reports-long-name.pdf");
        String longName = "Extremely Long Customer Name Private Limited";

        exporter.write(file, List.of(batch(1, longName)), "All batches (no filters applied)", null);

        try (PDDocument document = Loader.loadPDF(file)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).doesNotContain(longName);
            assertThat(text).contains("...");
        }
    }

    /**
     * The standard-14 fonts are WinAnsi-encoded and showText throws on anything
     * outside that. One awkward character must not fail the export.
     */
    @Test
    void survivesCharactersTheFontCannotEncode() throws IOException {
        File file = outputFile("batch-reports-odd-characters.pdf");

        exporter.write(file, List.of(batch(1, "Ṣurāj ✓ Co")), "All batches (no filters applied)", null);

        assertThat(file).exists();
        try (PDDocument document = Loader.loadPDF(file)) {
            assertThat(new PDFTextStripper().getText(document)).contains("250001");
        }
    }

    /** An empty result should say so, not hand over a blank page. */
    @Test
    void anEmptyResultSaysSo() throws IOException {
        File file = outputFile("batch-reports-empty.pdf");

        exporter.write(file, List.of(), "Filters: Client Nobody", null);

        try (PDDocument document = Loader.loadPDF(file)) {
            assertThat(new PDFTextStripper().getText(document)).contains("No batches matched these filters.");
        }
    }

    /** When the row cap is hit the PDF has to admit it. */
    @Test
    void printsTheNoteWhenNotEverythingFitted() throws IOException {
        File file = outputFile("batch-reports-capped.pdf");

        exporter.write(file, batches(3), "All batches (no filters applied)",
                "Showing the first 3 of 9000 matching batches - narrow the filters to export the rest.");

        try (PDDocument document = Loader.loadPDF(file)) {
            assertThat(new PDFTextStripper().getText(document)).contains("first 3 of 9000");
        }
    }
}
