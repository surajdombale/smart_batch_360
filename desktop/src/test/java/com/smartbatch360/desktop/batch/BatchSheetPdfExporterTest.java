package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.header.HeaderDto;
import com.smartbatch360.desktop.header.HeaderLogoDto;
import com.smartbatch360.desktop.header.HeaderStatus;
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
 * The single-batch report. Drawn by hand like the list export, so the same
 * things are worth pinning down - a long batch spilling onto further pages, a
 * name the font cannot encode - plus the arithmetic the sheet does itself: the
 * totals down the cycles, the variance, and the tonnes at the foot.
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
            assertThat(text).contains("Batch Report", "Company Name : SmartBatch Solutions", "Pin Code : 411014",
                    "GST Number : 27ABCDE1234F1Z5", "Suraj", "Kharadi", "251006001", "MH12PQ3457", "Ganesh More",
                    "M20 Concrete", "3.00 m3", "1.50 m3", "Cement", "Water", "TOTAL");
            // One cycle's row total, and the two cycles added down each column.
            assertThat(text).contains("301.00", "404.00", "198.00", "602.00");
        }
    }

    /**
     * What was weighed against what was set, as a percentage of what was set:
     * 404 against 400 is +1.00, 198 against 200 is -1.00, 602 against 600 is 0.33.
     */
    @Test
    void theVarianceIsMeasuredAgainstTheSetTotal() throws IOException {
        try (PDDocument document = exporter.build(batch(2, "Suraj"), cycles(2, "202.00", "99.00"), company, null)) {
            String text = textOf(document);

            assertThat(text).contains("Var %", "1.00", "-1.00", "0.33");
            assertThat(text).contains("Actual Produced Quantity : 0.60 TON");
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
            // Every cycle is there - each prints its own time, so count those.
            assertThat(text.split("300\\.00", -1).length - 1).isGreaterThanOrEqualTo(60);
            assertThat(text).contains("(continued)", "Page 1 of " + document.getNumberOfPages());
            // The whole load: 60 cycles of 300 kg is 18 tonnes.
            assertThat(text).contains("Actual Produced Quantity : 18.00 TON");

            // The totals stay on one page, so they can be read against each other.
            stripper.setStartPage(document.getNumberOfPages());
            String lastPage = stripper.getText(document);
            assertThat(lastPage).contains("Achieved", "Var %", "Actual Produced Quantity");
        }
    }

    @Test
    void aBatchWithNoCyclesStillPrintsAndSaysSo() throws IOException {
        try (PDDocument document = exporter.build(batch(2, "Suraj"), List.of(), null, null)) {
            String text = textOf(document);

            assertThat(text).contains("No cycles have been recorded for this batch yet.");
            assertThat(text).contains("Actual Produced Quantity : 0.00 TON");
            // No company row is not an error either - the block is simply blank.
            assertThat(text).contains("Company Name :");
        }
    }

    /** showText throws on a character the font cannot encode; one name must not fail the sheet. */
    @Test
    void aNameTheFontCannotEncodeDoesNotFailTheReport() throws IOException {
        try (PDDocument document = exporter.build(batch(1, "श्री Cement ☃"), cycles(1, "200.00", "100.00"),
                company, null)) {
            assertThat(textOf(document)).contains("Cement");
        }
    }

    /** A logo that cannot be decoded costs the report its picture, not itself. */
    @Test
    void anUnreadableLogoIsLeftOut() throws IOException {
        HeaderLogoDto broken = new HeaderLogoDto("image/png", new byte[] {1, 2, 3});

        try (PDDocument document = exporter.build(batch(1, "Suraj"), cycles(1, "200.00", "100.00"), company,
                broken)) {
            assertThat(textOf(document)).contains("Batch Report");
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
