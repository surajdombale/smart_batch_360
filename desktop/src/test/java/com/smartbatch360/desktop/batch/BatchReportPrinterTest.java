package com.smartbatch360.desktop.batch;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.awt.print.PageFormat;
import java.awt.print.Pageable;
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
 * Printing. No test can press Print on a real printer, so these pin down what
 * the printer is handed: every page of the report, landscape, and exactly the
 * document the PDF export writes - which is the promise that paper and file
 * cannot disagree.
 */
class BatchReportPrinterTest {

    private final BatchReportPdfExporter exporter = new BatchReportPdfExporter();

    private List<BatchDto> batches(int count) {
        List<BatchDto> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(new BatchDto(
                    (long) i, "250" + String.format("%03d", i),
                    2L, "M20 Concrete", null,
                    1L, "Suraj", 1L, "Kharadi", 1L, "MH12PQ3457", 1L, "Ganesh More",
                    new BigDecimal("310.00"), new BigDecimal("308.50"), new BigDecimal("1.50"),
                    Instant.parse("2026-09-14T06:30:00Z"), i, "Day",
                    BatchStatus.COMPLETED,
                    EquipmentStatus.STOPPED, EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                    EquipmentStatus.STOPPED, EquipmentStatus.STOPPED,
                    List.of(),
                    Instant.parse("2026-09-14T06:30:00Z"), Instant.parse("2026-09-14T06:30:00Z")));
        }
        return rows;
    }

    @Test
    void handsThePrinterEveryPageOfTheReport() throws IOException {
        try (PDDocument document = exporter.build(batches(60), "All batches (no filters applied)", null)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);

            Pageable pageable = BatchReportPrinter.pageableFor(document);

            assertThat(pageable.getNumberOfPages()).isEqualTo(document.getNumberOfPages());
        }
    }

    @Test
    void printsLandscapeLikeTheReportIsDrawn() throws IOException {
        try (PDDocument document = exporter.build(batches(5), "All batches (no filters applied)", null)) {
            PageFormat format = BatchReportPrinter.pageableFor(document).getPageFormat(0);

            assertThat(format.getOrientation()).isEqualTo(PageFormat.LANDSCAPE);
        }
    }

    /** build() is what gets printed and write() is what gets saved: they must be the same report. */
    @Test
    void printsTheSameDocumentTheExportSaves() throws IOException {
        Path dir = Path.of("target", "test-output");
        Files.createDirectories(dir);
        File saved = dir.resolve("batch-reports-print-parity.pdf").toFile();

        exporter.write(saved, batches(60), "All batches (no filters applied)", null);

        try (PDDocument printed = exporter.build(batches(60), "All batches (no filters applied)", null);
             PDDocument onDisk = Loader.loadPDF(saved)) {
            assertThat(printed.getNumberOfPages()).isEqualTo(onDisk.getNumberOfPages());
            assertThat(printed.getPage(0).getMediaBox().getWidth())
                    .isEqualTo(onDisk.getPage(0).getMediaBox().getWidth());
        }
    }
}
