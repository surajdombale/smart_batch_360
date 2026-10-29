package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.header.HeaderDto;
import com.smartbatch360.desktop.header.HeaderLogoDto;
import com.smartbatch360.desktop.header.HeaderStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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
 * The single-batch report, laid out after the user's sample of 28-Oct-2026.
 * Drawn by hand like the list export, so the same things are worth pinning
 * down - a long batch spilling onto further pages, a name the font cannot
 * encode - plus the arithmetic the sheet prints: the totals down the cycles,
 * and each material's difference from its target.
 *
 * Samples are left in target/test-output so the layout can be looked at.
 */
class BatchSheetPdfExporterTest {

    private final BatchSheetPdfExporter exporter = new BatchSheetPdfExporter();

    // Supervisor and mixer capacity moved to Settings > Plant Details on
    // 07-Oct-2026, so a company no longer carries them.
    private final HeaderDto company = new HeaderDto(1L, "SmartBatch Solutions", "Kharadi Plant", "Kharadi, Pune",
            "Pune", "411014", null, null, "27ABCDE1234F1Z5",
            false, HeaderStatus.ACTIVE, Instant.now(), Instant.now());

    /** A load of two materials: 200 kg and 100 kg a cycle. */
    private BatchDto batch(int cycles, String clientName) {
        return new BatchDto(7L, "251006001", 2L, "M20 Concrete", 15L, 1L, clientName, 1L, "Kharadi",
                1L, "MH12PQ3457", 1L, "Ganesh More",
                new BigDecimal(300 * cycles), BigDecimal.ZERO, BigDecimal.ZERO,
                Instant.parse("2026-10-06T06:30:00Z"), cycles, "Day", BatchStatus.COMPLETED,
                List.of(new BatchMaterialDto(1L, "Cement", new BigDecimal(200 * cycles), new BigDecimal("200.00"),
                                BigDecimal.ZERO, "kg"),
                        new BatchMaterialDto(2L, "Water", new BigDecimal(100 * cycles), new BigDecimal("100.00"),
                                BigDecimal.ZERO, "kg")),
                Instant.now(), Instant.now(), new BigDecimal("3.0000"), new BigDecimal("1.5000"));
    }

    private List<BatchCycleDto> cycles(int count, String cement, String water) {
        List<BatchCycleDto> cycles = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            cycles.add(new BatchCycleDto((long) i, i, Instant.parse("2026-10-06T06:31:00Z").plusSeconds(180L * i),
                    List.of(new BatchCycleDto.Material("Cement", null, null, new BigDecimal(cement)),
                            new BatchCycleDto.Material("Water", null, null, new BigDecimal(water))),
                    new BigDecimal(cement).add(new BigDecimal(water))));
        }
        return cycles;
    }

    private File outputFile(String name) throws IOException {
        Path dir = Path.of("target", "test-output");
        Files.createDirectories(dir);
        return dir.resolve(name).toFile();
    }

    private String textOf(PDDocument document) throws IOException {
        return new PDFTextStripper().getText(document);
    }

    @Test
    void printsTheLetterheadTheBatchAndEveryCycle() throws IOException {
        exporter.write(outputFile("batch-sheet.pdf"), batch(2, "Suraj"), cycles(2, "202.00", "99.00"), company, null);

        try (PDDocument document = exporter.build(batch(2, "Suraj"), cycles(2, "202.00", "99.00"), company, null)) {
            String text = textOf(document);

            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(text).contains("BATCH REPORT", "PRODUCTION BATCH SUMMARY", "Report Generated",
                    "SmartBatch Solutions", "411014", "27ABCDE1234F1Z5");
            assertThat(text).contains("1. GENERAL INFORMATION", "2. RECIPE / TARGET", "3. CYCLE DETAILS",
                    "4. BATCH SUMMARY", "REMARKS", "Approved By");
            assertThat(text).contains("Suraj", "Kharadi", "251006001", "MH12PQ3457", "Ganesh More", "M20 Concrete",
                    "3.00 m³", "Cement", "Water", "TOTAL");
            // One cycle's row total, and the two cycles added down each column.
            assertThat(text).contains("301.00", "404.00", "198.00", "602.00");
        }
    }

    /** A portrait page like the sample, for a recipe of ordinary size. */
    @Test
    void theReportIsPortraitA4() throws IOException {
        try (PDDocument document = exporter.build(batch(2, "Suraj"), cycles(2, "202.00", "99.00"), company, null)) {
            PDRectangle page = document.getPage(0).getMediaBox();
            assertThat(page.getHeight()).isGreaterThan(page.getWidth());
        }
    }

    /**
     * What was weighed against the load's target: 404 of cement against 400 is
     * +4.00, +1.00%; 198 of water against 200 is -2.00, -1.00%; the whole load,
     * 602 against 600, is +2.00, +0.33%.
     */
    @Test
    void eachMaterialIsMeasuredAgainstItsTarget() throws IOException {
        try (PDDocument document = exporter.build(batch(2, "Suraj"), cycles(2, "202.00", "99.00"), company, null)) {
            String text = textOf(document);

            assertThat(text).contains("+4.00", "+1.00%", "-2.00", "-1.00%", "+2.00", "+0.33%");
            // The setpoint total is what the two cycles were each set to.
            assertThat(text).contains("TARGET TOTAL (kg)", "SETPOINT TOTAL (kg)", "ACHIEVED TOTAL (kg)", "600.00");
        }
    }

    @Test
    void aLongBatchContinuesOntoFurtherPagesWithNothingDropped() throws IOException {
        File file = outputFile("batch-sheet-60-cycles.pdf");
        exporter.write(file, batch(60, "Suraj"), cycles(60, "200.00", "100.00"), company, null);

        try (PDDocument document = exporter.build(batch(60, "Suraj"), cycles(60, "200.00", "100.00"), company,
                null)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // Every cycle is there - each prints its own row total, so count those.
            assertThat(text.split("300\\.00", -1).length - 1).isGreaterThanOrEqualTo(60);
            assertThat(text).contains("(continued)", "Page 1 of " + document.getNumberOfPages());
            // The whole load: 60 cycles of 300 kg.
            assertThat(text).contains("18,000.00");

            // The summary and the totals stay together on the last page.
            stripper.setStartPage(document.getNumberOfPages());
            String lastPage = stripper.getText(document);
            assertThat(lastPage).contains("4. BATCH SUMMARY", "ACHIEVED TOTAL (kg)", "REMARKS");
        }
    }

    @Test
    void aBatchWithNoCyclesStillPrintsAndSaysSo() throws IOException {
        try (PDDocument document = exporter.build(batch(2, "Suraj"), List.of(), null, null)) {
            String text = textOf(document);

            assertThat(text).contains("No cycles have been recorded for this batch yet.");
            // No company row is not an error either - the letterhead falls back to SmartBatch360.
            assertThat(text).contains("SmartBatch360", "BATCH REPORT");
        }
    }

    /** More materials than a portrait page can show readably turns the page. */
    @Test
    void aLargeRecipeIsPrintedLandscape() throws IOException {
        List<BatchMaterialDto> materials = new ArrayList<>();
        List<BatchCycleDto.Material> weighed = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            materials.add(new BatchMaterialDto((long) i, "Material " + i, new BigDecimal("20.00"),
                    new BigDecimal("10.00"), BigDecimal.ZERO, "kg"));
            weighed.add(new BatchCycleDto.Material("Material " + i, null, null, new BigDecimal("10.00")));
        }
        BatchDto base = batch(1, "Suraj");
        BatchDto wide = new BatchDto(base.id(), base.batchNumber(), base.recipeId(), base.recipeName(), base.orderId(),
                base.clientId(), base.clientName(), base.siteId(), base.siteName(), base.vehicleId(),
                base.vehicleNumber(), base.driverId(), base.driverName(), base.targetQuantity(),
                base.producedQuantity(), base.remainingQuantity(), base.cycleDateTime(), base.cycleNumber(),
                base.shift(), base.status(), materials, base.createdAt(), base.updatedAt(), base.batchSizeM3(),
                base.perCycleM3());
        List<BatchCycleDto> cycles = List.of(new BatchCycleDto(1L, 1, Instant.parse("2026-10-06T06:31:00Z"), weighed,
                new BigDecimal("120.00")));

        exporter.write(outputFile("batch-sheet-12-materials.pdf"), wide, cycles, company, null);
        try (PDDocument document = exporter.build(wide, cycles, company, null)) {
            PDRectangle page = document.getPage(0).getMediaBox();
            assertThat(page.getWidth()).isGreaterThan(page.getHeight());
            assertThat(textOf(document)).contains("Material 12");
        }
    }

    /**
     * Twenty materials - the most a recipe carries - still go across one
     * landscape page, every figure printed: the recipe's target and setpoint,
     * the cycle, and the batch total beneath it, for each of the twenty.
     */
    @Test
    void twentyMaterialsStillFitAcrossTheLandscapePage() throws IOException {
        List<BatchMaterialDto> materials = new ArrayList<>();
        List<BatchCycleDto.Material> weighed = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            materials.add(new BatchMaterialDto((long) i, "Crushed Sand " + i, new BigDecimal("10.00"),
                    new BigDecimal("10.00"), BigDecimal.ZERO, "kg"));
            weighed.add(new BatchCycleDto.Material("Crushed Sand " + i, null, null, new BigDecimal("10.00")));
        }
        BatchDto base = batch(1, "Suraj");
        BatchDto wide = new BatchDto(base.id(), base.batchNumber(), base.recipeId(), base.recipeName(), base.orderId(),
                base.clientId(), base.clientName(), base.siteId(), base.siteName(), base.vehicleId(),
                base.vehicleNumber(), base.driverId(), base.driverName(), base.targetQuantity(),
                base.producedQuantity(), base.remainingQuantity(), base.cycleDateTime(), base.cycleNumber(),
                base.shift(), base.status(), materials, base.createdAt(), base.updatedAt(), base.batchSizeM3(),
                base.perCycleM3());
        List<BatchCycleDto> cycles = List.of(new BatchCycleDto(1L, 1, Instant.parse("2026-10-06T06:31:00Z"), weighed,
                new BigDecimal("200.00")));

        exporter.write(outputFile("batch-sheet-20-materials.pdf"), wide, cycles, company, null);
        try (PDDocument document = exporter.build(wide, cycles, company, null)) {
            PDRectangle page = document.getPage(0).getMediaBox();
            assertThat(page.getWidth()).isGreaterThan(page.getHeight());

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(1);
            String firstPage = stripper.getText(document);
            // Target, setpoint, the cycle and the batch total - four for each material.
            assertThat(firstPage.split("10\\.00", -1).length - 1).isGreaterThanOrEqualTo(80);
            assertThat(firstPage).contains("200.00");
        }
    }

    /** showText throws on a character the font cannot encode; one name must not fail the sheet. */
    @Test
    void aNameTheFontCannotEncodeDoesNotFailTheReport() throws IOException {
        try (PDDocument document = exporter.build(batch(1, "श्री Cement ☃"),
                cycles(1, "200.00", "100.00"), company, null)) {
            assertThat(textOf(document)).contains("Cement");
        }
    }

    /** A logo that cannot be decoded costs the report its picture, not itself. */
    @Test
    void anUnreadableLogoIsLeftOut() throws IOException {
        HeaderLogoDto broken = new HeaderLogoDto("image/png", new byte[] {1, 2, 3});

        try (PDDocument document = exporter.build(batch(1, "Suraj"), cycles(1, "200.00", "100.00"), company,
                broken)) {
            assertThat(textOf(document)).contains("BATCH REPORT");
        }
    }

    /** A material a cycle reports that the batch never planned is printed, with nothing to compare it to. */
    @Test
    void anUnplannedMaterialGetsItsOwnColumn() throws IOException {
        List<BatchCycleDto> cycles = List.of(new BatchCycleDto(1L, 1, Instant.parse("2026-10-06T06:31:00Z"),
                List.of(new BatchCycleDto.Material("Cement", null, null, new BigDecimal("200.00")),
                        new BatchCycleDto.Material("Fly Ash", null, null, new BigDecimal("40.00"))),
                new BigDecimal("240.00")));

        try (PDDocument document = exporter.build(batch(1, "Suraj"), cycles, company, null)) {
            assertThat(textOf(document)).contains("Fly Ash", "40.00");
        }
    }
}
