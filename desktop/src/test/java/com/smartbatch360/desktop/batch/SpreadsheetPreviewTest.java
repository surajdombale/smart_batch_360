package com.smartbatch360.desktop.batch;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** The Excel preview shows each value as Excel would under the cell's number format. */
class SpreadsheetPreviewTest {

    private SpreadsheetLayout.Cell cell(Object value, String format) {
        SpreadsheetLayout layout = new SpreadsheetLayout("Sheet");
        layout.value(0, 0, value);
        if (format != null) {
            layout.format(0, 0, format);
        }
        return layout.cell(0, 0);
    }

    /** Excel's General format drops trailing zeros: 600.00 is shown as 600. */
    @Test
    void aNumberWithNoFormatIsShownAsExcelsGeneralShowsIt() {
        assertThat(SpreadsheetPreview.display(cell(new BigDecimal("600.00"), null))).isEqualTo("600");
        assertThat(SpreadsheetPreview.display(cell(new BigDecimal("3600.010"), null))).isEqualTo("3600.01");
        assertThat(SpreadsheetPreview.display(cell(new BigDecimal("0.00"), null))).isEqualTo("0");
        assertThat(SpreadsheetPreview.display(cell(3, null))).isEqualTo("3");
    }

    @Test
    void aTwoDecimalFormatRoundsToTwoPlaces() {
        assertThat(SpreadsheetPreview.display(cell(new BigDecimal("0.5"), "0.00"))).isEqualTo("0.50");
        assertThat(SpreadsheetPreview.display(cell(new BigDecimal("-1.23456"), "0.00"))).isEqualTo("-1.23");
    }

    /** Excel's mm means minutes after hh and months after yyyy - not the same letters as Java's. */
    @Test
    void datesFollowTheirExcelFormat() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 28, 14, 16, 6);

        assertThat(SpreadsheetPreview.display(cell(time, "yyyy-mm-dd hh:mm:ss"))).isEqualTo("2026-10-28 14:16:06");
        assertThat(SpreadsheetPreview.display(cell(time, "hh:mm:ss"))).isEqualTo("14:16:06");
    }

    /** A formula's cell shows what it works out to, as Excel does once it opens the file. */
    @Test
    void aFormulaShowsItsResult() {
        SpreadsheetLayout layout = new SpreadsheetLayout("Sheet");
        layout.formula(0, 0, "SUM(A2:A4)", new BigDecimal("603.00"));
        layout.formula(1, 0, "IF(A5=0,\"\",1)", "");

        assertThat(SpreadsheetPreview.display(layout.cell(0, 0))).isEqualTo("603");
        assertThat(SpreadsheetPreview.display(layout.cell(1, 0))).isEmpty();
    }
}
