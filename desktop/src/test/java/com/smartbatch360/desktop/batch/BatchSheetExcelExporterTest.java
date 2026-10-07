package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.header.HeaderDto;
import com.smartbatch360.desktop.header.HeaderStatus;
import org.dhatim.fastexcel.reader.Cell;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One batch's report as a spreadsheet. The grid is what matters - cycles down,
 * materials across - and the figures have to be numbers, not text, or the
 * totals a reader adds up will not agree with the ones printed.
 */
class BatchSheetExcelExporterTest {

    private final BatchSheetExcelExporter exporter = new BatchSheetExcelExporter();

    private final HeaderDto company = new HeaderDto(1L, "SmartBatch Solutions", "Kharadi Plant", "Kharadi, Pune",
            "Pune", "411014", null, null, "27ABCDE1234F1Z5",
            false, HeaderStatus.ACTIVE, Instant.now(), Instant.now());

    /** Two materials set to 200 and 100 kg a cycle, over three cycles. */
    private BatchDto batch() {
        return new BatchDto(1L, "250401", 2L, "M25", 9L, 1L, "Shree Cement", 1L, "Kharadi",
                1L, "MH12PQ3457", 1L, "Ganesh More",
                new BigDecimal("900.00"), new BigDecimal("900.00"), BigDecimal.ZERO,
                Instant.parse("2026-10-07T06:30:00Z"), 3, null, BatchStatus.COMPLETED,
                List.of(new BatchMaterialDto(1L, "Cement", new BigDecimal("600.00"), new BigDecimal("200.00"),
                                new BigDecimal("0"), "kg"),
                        new BatchMaterialDto(2L, "Water", new BigDecimal("300.00"), new BigDecimal("100.00"),
                                new BigDecimal("0"), "kg")),
                Instant.now(), Instant.now());
    }

    private List<BatchCycleDto> cycles() {
        return List.of(
                cycle(1, "06:31:00", "201", "101"),
                cycle(2, "06:32:00", "202", "100"),
                cycle(3, "06:33:00", "200", "99"));
    }

    private BatchCycleDto cycle(int number, String time, String cement, String water) {
        return new BatchCycleDto((long) number, number, Instant.parse("2026-10-07T" + time + "Z"),
                List.of(new BatchCycleDto.Material("Cement", new BigDecimal("600.00"), new BigDecimal("200.00"),
                                new BigDecimal(cement)),
                        new BatchCycleDto.Material("Water", new BigDecimal("300.00"), new BigDecimal("100.00"),
                                new BigDecimal(water))),
                new BigDecimal(cement).add(new BigDecimal(water)));
    }

    private File write(BatchDto batch, List<BatchCycleDto> cycles) throws IOException {
        Path dir = Path.of("target", "test-output");
        Files.createDirectories(dir);
        File file = dir.resolve("batch-report-" + batch.batchNumber() + ".xlsx").toFile();
        exporter.write(file, batch, cycles, company);
        return file;
    }

    private List<Row> rows(File file) throws IOException {
        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            Sheet sheet = workbook.getFirstSheet();
            return sheet.read();
        }
    }

    /**
     * The first column holds labels on some rows and a cycle number on others,
     * so it is read as a raw value - asking for a string on a numeric cell
     * throws.
     */
    private Optional<Row> rowStartingWith(List<Row> rows, String label) {
        return rows.stream()
                .filter(r -> r.getCellCount() > 0 && label.equalsIgnoreCase(firstCellText(r)))
                .findFirst();
    }

    private String firstCellText(Row row) {
        Cell cell = row.getCell(0);
        if (cell == null || cell.getValue() == null) {
            return "";
        }
        return String.valueOf(cell.getValue()).trim();
    }

    @Test
    void theReportIsOneSheetNamedForWhatItIs() throws IOException {
        File file = write(batch(), cycles());

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            assertThat(workbook.getSheets().map(Sheet::getName).toList()).containsExactly("Batch Report");
        }
    }

    @Test
    void everyCycleGetsARowWithItsMaterials() throws IOException {
        List<Row> rows = rows(write(batch(), cycles()));

        Optional<Row> firstCycle = rowStartingWith(rows, "1");
        assertThat(firstCycle).isPresent();
        assertThat(firstCycle.get().getCellAsNumber(2).orElseThrow()).isEqualByComparingTo(new BigDecimal("201"));
        assertThat(firstCycle.get().getCellAsNumber(3).orElseThrow()).isEqualByComparingTo(new BigDecimal("101"));
    }

    /**
     * The figures have to be numbers. Written as text they would look right and
     * refuse to add up, which is worse than being absent.
     */
    @Test
    void theAchievedFiguresAreNumbersNotText() throws IOException {
        List<Row> rows = rows(write(batch(), cycles()));

        Row cycleRow = rowStartingWith(rows, "2").orElseThrow();
        assertThat(cycleRow.getCellAsNumber(2)).isPresent();
        assertThat(cycleRow.getCellAsNumber(3)).isPresent();
    }

    @Test
    void theTargetAndSetpointRowsComeFromTheBatch() throws IOException {
        List<Row> rows = rows(write(batch(), cycles()));

        Row target = rowStartingWith(rows, "Target").orElseThrow();
        assertThat(target.getCellAsNumber(2).orElseThrow()).isEqualByComparingTo(new BigDecimal("600.00"));

        Row setpoint = rowStartingWith(rows, "Setpoint").orElseThrow();
        assertThat(setpoint.getCellAsNumber(2).orElseThrow()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    /** Set total is the setpoint multiplied by the cycles actually run. */
    @Test
    void theSetTotalIsTheSetpointTimesTheCycleCount() throws IOException {
        List<Row> rows = rows(write(batch(), cycles()));

        Row setTotal = rowStartingWith(rows, "Set total").orElseThrow();
        assertThat(setTotal.getCellAsNumber(2).orElseThrow()).isEqualByComparingTo(new BigDecimal("600"));
        assertThat(setTotal.getCellAsNumber(3).orElseThrow()).isEqualByComparingTo(new BigDecimal("300"));
    }

    @Test
    void theCompanyLetterheadIsOnIt() throws IOException {
        List<Row> rows = rows(write(batch(), cycles()));

        assertThat(rows.get(0).getCellAsString(0).orElse("")).isEqualTo("SmartBatch Solutions");
        assertThat(rows.stream().anyMatch(r -> r.getCellCount() > 0
                && r.getCellAsString(0).orElse("").contains("411014"))).isTrue();
    }

    /** A batch whose cycles have not been reported yet is still a valid report. */
    @Test
    void abatchWithNoCyclesIsStillAWorkbook() throws IOException {
        File file = write(batch(), List.of());

        List<Row> rows = rows(file);
        assertThat(rows).isNotEmpty();
        assertThat(rowStartingWith(rows, "Target")).isPresent();
        // Nothing to total, so those rows are left off rather than summing an empty range.
        assertThat(rowStartingWith(rows, "Achieved total")).isEmpty();
    }
}
