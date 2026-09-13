package com.smartbatch360.desktop.batch;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
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
 * The Excel export. Its whole reason to exist beside the PDF is that figures
 * stay figures, so the tests read the workbook back and check cells are real
 * numbers and dates - a sheet of numbers stored as text looks identical on
 * screen and silently breaks every SUM and sort.
 *
 * Samples are left in target/test-output to open by hand.
 */
class BatchReportExcelExporterTest {

    private final BatchReportExcelExporter exporter = new BatchReportExcelExporter();

    private BatchDto batch(int index, String clientName, List<BatchMaterialDto> materials) {
        return new BatchDto(
                (long) index,
                "250" + String.format("%03d", index),
                2L, "M20 Concrete",
                7L,
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
                materials,
                Instant.parse("2026-09-12T06:30:00Z"), Instant.parse("2026-09-12T06:30:00Z"));
    }

    private List<BatchMaterialDto> cementAndWater() {
        return List.of(
                new BatchMaterialDto(1L, "Cement", new BigDecimal("50.00"), new BigDecimal("50.00"),
                        new BigDecimal("52.00"), "kg"),
                new BatchMaterialDto(2L, "Water", new BigDecimal("10.00"), new BigDecimal("10.00"),
                        new BigDecimal("9.50"), "kg"));
    }

    private File outputFile(String name) throws IOException {
        Path dir = Path.of("target", "test-output");
        Files.createDirectories(dir);
        return dir.resolve(name).toFile();
    }

    private List<Row> rows(File file, String sheetName) throws IOException {
        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            Sheet sheet = workbook.getSheets()
                    .filter(s -> s.getName().equals(sheetName))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no sheet named " + sheetName));
            return new ArrayList<>(sheet.read());
        }
    }

    @Test
    void hasTheThreeSheets() throws IOException {
        File file = outputFile("batch-reports-sheets.xlsx");
        exporter.write(file, List.of(batch(1, "Suraj", cementAndWater())), "All batches (no filters applied)", null);

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            assertThat(workbook.getSheets().map(Sheet::getName).toList())
                    .containsExactly("Batches", "Materials", "About");
        }
    }

    /** The reason for the spreadsheet: numbers and dates as real cells, not text. */
    @Test
    void writesQuantitiesAsNumbersAndCycleTimesAsDates() throws IOException {
        File file = outputFile("batch-reports-sample.xlsx");
        exporter.write(file, List.of(batch(1, "Suraj", cementAndWater()), batch(2, "Suraj", cementAndWater())),
                "All batches (no filters applied)", null);

        List<Row> batches = rows(file, "Batches");
        assertThat(batches).hasSize(3);   // header + two batches
        assertThat(batches.get(0).getCellText(0)).isEqualTo("Batch Number");
        assertThat(batches.get(0).getCellText(9)).isEqualTo("Target (kg)");

        Row first = batches.get(1);
        assertThat(first.getCellText(0)).isEqualTo("250001");
        assertThat(first.getCellAsDate(1)).isPresent();
        assertThat(first.getCellAsNumber(9)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("310.00"));
        assertThat(first.getCellAsNumber(10)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("308.50"));
        assertThat(first.getCellAsNumber(13)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("7"));
        assertThat(first.getCellText(12)).isEqualTo("COMPLETED");
    }

    @Test
    void listsEachMaterialWithItsVariance() throws IOException {
        File file = outputFile("batch-reports-materials.xlsx");
        exporter.write(file, List.of(batch(1, "Suraj", cementAndWater())), "All batches (no filters applied)", null);

        List<Row> materials = rows(file, "Materials");
        assertThat(materials).hasSize(3);   // header + cement + water

        Row cement = materials.get(1);
        assertThat(cement.getCellText(1)).isEqualTo("Cement");
        // 52 used against a target of 50: over by 2, the same sign the
        // Material Consumption screen uses for over-use.
        assertThat(cement.getCellAsNumber(5)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("2.00"));

        Row water = materials.get(2);
        assertThat(water.getCellAsNumber(5)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("-0.50"));
    }

    @Test
    void recordsTheFiltersAndTheCapOnTheAboutSheet() throws IOException {
        File file = outputFile("batch-reports-about.xlsx");
        exporter.write(file, List.of(batch(1, "Suraj", List.of())), "Filters: Client Suraj",
                "Showing the first 1 of 9000 matching batches - narrow the filters to export the rest.");

        List<Row> about = rows(file, "About");
        String everything = about.stream()
                .map(r -> r.getCellText(0) + " " + r.getCellText(1))
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(everything).contains("Filters: Client Suraj", "first 1 of 9000");
    }

    /** No batches is still a valid workbook, headings in place. */
    @Test
    void anEmptyResultIsStillAValidWorkbook() throws IOException {
        File file = outputFile("batch-reports-empty.xlsx");
        exporter.write(file, List.of(), "Filters: Client Nobody", null);

        assertThat(rows(file, "Batches")).hasSize(1);
        assertThat(rows(file, "Materials")).hasSize(1);
    }

    /**
     * XML 1.0 cannot carry control characters, and one in a customer name would
     * make Excel reject the entire file as corrupt.
     */
    @Test
    void dropsControlCharactersRatherThanCorruptingTheFile() throws IOException {
        File file = outputFile("batch-reports-control-chars.xlsx");
        // Written as an escape sequence rather than the raw character, so it is visible.
        exporter.write(file, List.of(batch(1, "Sur\u0001aj", List.of())), "All batches (no filters applied)", null);

        List<Row> batches = rows(file, "Batches");
        assertThat(batches.get(1).getCellText(3)).isEqualTo("Suraj");
    }
}
