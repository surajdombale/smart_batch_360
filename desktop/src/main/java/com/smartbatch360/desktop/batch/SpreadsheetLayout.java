package com.smartbatch360.desktop.batch;

import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Worksheet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * A worksheet's cells, laid out once, so the workbook a user downloads and the
 * preview they looked at first are drawn from the same thing and cannot
 * disagree (Batch Reports > View, 31-Oct-2026).
 *
 * It records what fastexcel would be told - values, formulas, bold, font size,
 * number format, column widths, frozen rows - and replays it onto a Worksheet.
 * A formula carries the value it works out to as well: fastexcel writes the
 * formula alone and leaves Excel to calculate it on opening, and the preview
 * has no Excel to ask.
 */
final class SpreadsheetLayout {

    /** One cell. Its value is a String, a Number or a LocalDateTime, or null when only styled. */
    static final class Cell {
        private Object value;
        private String formula;
        private String format;
        private boolean bold;
        private int fontSize;   // 0 = the workbook's own default

        Object value() {
            return value;
        }

        String formula() {
            return formula;
        }

        String format() {
            return format;
        }

        boolean bold() {
            return bold;
        }

        int fontSize() {
            return fontSize;
        }

        private boolean styled() {
            return bold || fontSize > 0 || format != null;
        }
    }

    private final String sheetName;
    private final NavigableMap<Integer, NavigableMap<Integer, Cell>> rows = new TreeMap<>();
    private final Map<Integer, Double> widths = new TreeMap<>();
    private int frozenRows;

    SpreadsheetLayout(String sheetName) {
        this.sheetName = sheetName;
    }

    void value(int row, int column, Object value) {
        cellAt(row, column).value = value;
    }

    /** @param result what the formula works out to, for anything that reads the layout rather than Excel */
    void formula(int row, int column, String formula, Object result) {
        Cell cell = cellAt(row, column);
        cell.formula = formula;
        cell.value = result;
    }

    void bold(int row, int column) {
        cellAt(row, column).bold = true;
    }

    void bold(int row, int fromColumn, int toColumn) {
        for (int column = fromColumn; column <= toColumn; column++) {
            bold(row, column);
        }
    }

    void fontSize(int row, int column, int size) {
        cellAt(row, column).fontSize = size;
    }

    void format(int row, int column, String format) {
        cellAt(row, column).format = format;
    }

    /** In Excel's unit: the width of that many digits in the default font. */
    void width(int column, double width) {
        widths.put(column, width);
    }

    /** Keeps the rows above {@code rows} in view while the rest scroll. */
    void freezeRows(int rows) {
        this.frozenRows = rows;
    }

    String sheetName() {
        return sheetName;
    }

    /** The cell at that place, or null if nothing was ever put there. */
    Cell cell(int row, int column) {
        NavigableMap<Integer, Cell> cells = rows.get(row);
        return cells == null ? null : cells.get(column);
    }

    /** A cell's number - a formula's result counts - or null when it holds none. */
    BigDecimal number(int row, int column) {
        Cell cell = cell(row, column);
        if (cell == null || !(cell.value instanceof Number number)) {
            return null;
        }
        return number instanceof BigDecimal decimal ? decimal : new BigDecimal(number.toString());
    }

    /** -1 when the sheet is empty. */
    int lastRow() {
        return rows.isEmpty() ? -1 : rows.lastKey();
    }

    /** -1 when the sheet is empty. */
    int lastColumn() {
        int last = -1;
        for (NavigableMap<Integer, Cell> cells : rows.values()) {
            if (!cells.isEmpty()) {
                last = Math.max(last, cells.lastKey());
            }
        }
        return last;
    }

    /** The width set for a column, or null for Excel's default. */
    Double width(int column) {
        return widths.get(column);
    }

    int frozenRows() {
        return frozenRows;
    }

    void writeTo(Worksheet sheet) {
        rows.forEach((row, cells) -> cells.forEach((column, cell) -> {
            if (cell.formula != null) {
                sheet.formula(row, column, cell.formula);
            } else if (cell.value instanceof String text) {
                sheet.value(row, column, text);
            } else if (cell.value instanceof Number number) {
                sheet.value(row, column, number);
            } else if (cell.value instanceof LocalDateTime time) {
                sheet.value(row, column, time);
            }
            if (cell.styled()) {
                StyleSetter style = sheet.style(row, column);
                if (cell.bold) {
                    style = style.bold();
                }
                if (cell.fontSize > 0) {
                    style = style.fontSize(cell.fontSize);
                }
                if (cell.format != null) {
                    style = style.format(cell.format);
                }
                style.set();
            }
        }));
        widths.forEach(sheet::width);
        if (frozenRows > 0) {
            sheet.freezePane(0, frozenRows);
        }
    }

    private Cell cellAt(int row, int column) {
        return rows.computeIfAbsent(row, r -> new TreeMap<>()).computeIfAbsent(column, c -> new Cell());
    }
}
