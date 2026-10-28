package com.smartbatch360.desktop.batch;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The grid both the printed batch report and the View dialog show. Pinned
 * here line by line, because the dialog has nothing else checking it.
 */
class BatchSheetTest {

    /** Two materials set to 200 and 100 kg a cycle, over a planned two cycles. */
    private BatchDto batch() {
        return new BatchDto(7L, "251006001", 2L, "M20 Concrete", 15L, 1L, "Suraj", 1L, "Kharadi",
                1L, "MH12PQ3457", 1L, "Ganesh More",
                new BigDecimal("600"), BigDecimal.ZERO, BigDecimal.ZERO,
                Instant.parse("2026-10-06T06:30:00Z"), 2, null, BatchStatus.COMPLETED,
                List.of(new BatchMaterialDto(1L, "Cement", new BigDecimal("400.00"), new BigDecimal("200.00"),
                                BigDecimal.ZERO, "kg"),
                        new BatchMaterialDto(2L, "Water", new BigDecimal("200.00"), new BigDecimal("100.00"),
                                BigDecimal.ZERO, "kg")),
                Instant.now(), Instant.now(), new BigDecimal("2.0000"), new BigDecimal("1.0000"));
    }

    private BatchCycleDto cycle(int number, String cement, String water) {
        return new BatchCycleDto((long) number, number, Instant.parse("2026-10-06T06:31:00Z").plusSeconds(180L * number),
                List.of(new BatchCycleDto.Material("Cement", null, null, new BigDecimal(cement)),
                        new BatchCycleDto.Material("Water", null, null, new BigDecimal(water))),
                new BigDecimal(cement).add(new BigDecimal(water)));
    }

    @Test
    void theGridReadsTargetSetTheCyclesThenTheLoadTotals() {
        BatchSheet sheet = BatchSheet.of(batch(), List.of(cycle(1, "202.00", "99.00"), cycle(2, "199.00", "101.00")));

        assertThat(sheet.headings().cells()).containsExactly("No", "Time", "Cement", "Water", "TOTAL");
        assertThat(sheet.body()).extracting(r -> r.cells().get(0))
                .containsExactly("Target", "Set", "1", "2", "Achieved", "Set", "Var %");
        assertThat(sheet.body()).extracting(BatchSheet.Row::shaded)
                .containsExactly(true, true, false, false, true, true, true);
    }

    @Test
    void eachCycleCarriesWhatItWeighedAndItsOwnTotal() {
        BatchSheet sheet = BatchSheet.of(batch(), List.of(cycle(1, "202.00", "99.00"), cycle(2, "199.00", "101.00")));

        assertThat(sheet.cycleRows().get(0).cells()).containsSubsequence("202.00", "99.00", "301.00");
        assertThat(sheet.cycleRows().get(1).cells()).containsSubsequence("199.00", "101.00", "300.00");
    }

    /** Per cycle above the cycles, the whole load below them. */
    @Test
    void theTotalsAreTheWholeLoadAgainstWhatWasSet() {
        BatchSheet sheet = BatchSheet.of(batch(), List.of(cycle(1, "202.00", "99.00"), cycle(2, "199.00", "101.00")));

        assertThat(sheet.perCycleRows().get(1).cells()).containsExactly("Set", "", "200.00", "100.00", "300.00");
        assertThat(sheet.totalRows().get(0).cells()).containsExactly("Achieved", "", "401.00", "200.00", "601.00");
        assertThat(sheet.totalRows().get(1).cells()).containsExactly("Set", "", "400.00", "200.00", "600.00");
        assertThat(sheet.totalRows().get(2).cells()).containsExactly("Var %", "", "0.25", "0.00", "0.17");
        assertThat(sheet.producedTonnes()).isEqualTo("0.60");
    }

    /** A batch whose cycles have not arrived still shows what it is aiming at. */
    @Test
    void aBatchWithNoCyclesStillHasItsPlan() {
        BatchSheet sheet = BatchSheet.of(batch(), List.of());

        assertThat(sheet.hasCycles()).isFalse();
        assertThat(sheet.cycleRows()).isEmpty();
        assertThat(sheet.perCycleRows().get(0).cells()).containsExactly("Target", "", "200.00", "100.00", "300.00");
        assertThat(sheet.producedTonnes()).isEqualTo("0.00");
    }
}
