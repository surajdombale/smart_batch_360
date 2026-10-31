package com.smartbatch360.desktop.batch;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A SpreadsheetLayout drawn as Excel shows it - lettered columns, numbered
 * rows, gridlines, Excel's column widths and its default font - so Batch
 * Reports > View can show what the downloaded workbook holds before it is
 * saved. Drawn from the same layout the workbook is written from.
 *
 * Text runs on over empty cells to its right, as Excel's does, and is cut at
 * the next cell that holds something. Numbers and dates never run on.
 */
final class SpreadsheetPreview {

    /** Excel's unit of column width is one digit of its default font, Calibri 11: 7 pixels. */
    private static final double DIGIT_WIDTH = 7;
    private static final double CELL_PADDING = 5;
    /** A cell's padding and gridline (theme.css .sheet-cell), with a pixel to spare. */
    private static final double TEXT_INSET = 8;
    private static final double DEFAULT_COLUMN_WIDTH = 8.43;
    /** Excel's default row, 15 points, in pixels. */
    private static final double DEFAULT_ROW_HEIGHT = 20;
    private static final String FONT_FAMILY = "Calibri";
    private static final int DEFAULT_FONT_POINTS = 11;
    private static final double ROW_HEADER_WIDTH = 40;
    /** Empty rows and columns past the last used one, so it reads as a sheet rather than a table. */
    private static final int SPARE_ROWS = 6;
    private static final int SPARE_COLUMNS = 3;

    private SpreadsheetPreview() {
    }

    static Region of(SpreadsheetLayout layout) {
        int lastRow = Math.max(layout.lastRow(), 0) + SPARE_ROWS;
        int lastColumn = Math.max(layout.lastColumn(), 0) + SPARE_COLUMNS;

        GridPane grid = new GridPane();
        grid.getStyleClass().add("sheet-grid");

        // Column 0 and row 0 of the grid are Excel's headings; the sheet starts at 1, 1.
        double[] widths = new double[lastColumn + 1];
        grid.getColumnConstraints().add(fixedColumn(ROW_HEADER_WIDTH));
        for (int column = 0; column <= lastColumn; column++) {
            widths[column] = pixels(layout.width(column));
            grid.getColumnConstraints().add(fixedColumn(widths[column]));
        }
        grid.getRowConstraints().add(fixedRow(DEFAULT_ROW_HEIGHT));
        for (int row = 0; row <= lastRow; row++) {
            grid.getRowConstraints().add(fixedRow(rowHeight(layout, row, lastColumn)));
        }

        grid.add(heading("", "sheet-corner"), 0, 0);
        for (int column = 0; column <= lastColumn; column++) {
            grid.add(heading(columnLetter(column), "sheet-column-heading"), column + 1, 0);
        }

        for (int row = 0; row <= lastRow; row++) {
            grid.add(heading(String.valueOf(row + 1), "sheet-row-heading"), 0, row + 1);
            int column = 0;
            while (column <= lastColumn) {
                SpreadsheetLayout.Cell cell = layout.cell(row, column);
                Font font = font(cell);
                String text = display(cell);
                int span = 1;
                if (cell != null && cell.value() instanceof String && !text.isEmpty()) {
                    double needed = textWidth(text, font) + TEXT_INSET;
                    double room = widths[column];
                    while (room < needed && column + span <= lastColumn && isEmpty(layout.cell(row, column + span))) {
                        room += widths[column + span];
                        span++;
                    }
                }
                grid.add(cell(cell, text, font), column + 1, row + 1, span, 1);
                column += span;
            }
        }

        // One tab, as Excel shows the sheets of a workbook beneath it.
        Label tab = new Label(layout.sheetName());
        tab.getStyleClass().add("sheet-tab");
        HBox tabs = new HBox(tab);
        tabs.getStyleClass().add("sheet-tabs");

        VBox sheet = new VBox(grid, tabs);
        sheet.getStyleClass().add("sheet");
        sheet.setFillWidth(false);
        return sheet;
    }

    /** What Excel would show in the cell, under its number format. */
    static String display(SpreadsheetLayout.Cell cell) {
        if (cell == null || cell.value() == null) {
            return "";
        }
        Object value = cell.value();
        if (value instanceof LocalDateTime time) {
            return DateTimeFormatter.ofPattern(javaPattern(cell.format())).format(time);
        }
        if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            BigDecimal number = new BigDecimal(value.toString());
            if (cell.format() != null && cell.format().matches("0\\.0+")) {
                return number.setScale(cell.format().length() - 2, RoundingMode.HALF_UP).toPlainString();
            }
            // Excel's General format: no trailing zeros, so 600.00 shows as 600.
            return number.signum() == 0 ? "0" : number.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    /** The two date formats the batch sheet uses, in Java's letters: Excel's mm is minutes after hh. */
    private static String javaPattern(String excelFormat) {
        if (excelFormat == null) {
            return "yyyy-MM-dd HH:mm:ss";
        }
        return excelFormat.replace("yyyy-mm-dd", "yyyy-MM-dd").replace("hh", "HH");
    }

    private static Label cell(SpreadsheetLayout.Cell cell, String text, Font font) {
        Label label = new Label(text);
        label.getStyleClass().add("sheet-cell");
        // Inline, and as the -fx-font shorthand: the theme gives every node
        // Segoe UI, which outranks a font set from code and even an inline
        // -fx-font-family (measured), and Segoe runs wider than Calibri, so
        // text Excel fits was being cut short.
        label.setStyle("-fx-font: " + (cell != null && cell.bold() ? "bold " : "") + font.getSize() + "px \""
                + FONT_FAMILY + "\";");
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        boolean figure = cell != null && (cell.value() instanceof Number || cell.value() instanceof LocalDateTime);
        label.setAlignment(figure ? Pos.BOTTOM_RIGHT : Pos.BOTTOM_LEFT);
        GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setVgrow(label, Priority.ALWAYS);
        return label;
    }

    private static Label heading(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("sheet-heading", styleClass);
        label.setAlignment(Pos.CENTER);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return label;
    }

    private static double textWidth(String text, Font font) {
        Text measuring = new Text(text);
        measuring.setFont(font);
        return measuring.getLayoutBounds().getWidth();
    }

    private static boolean isEmpty(SpreadsheetLayout.Cell cell) {
        return cell == null || cell.value() == null || "".equals(cell.value());
    }

    /** Excel's own font, in pixels at 96 DPI - a point is 4/3 of a pixel there. */
    private static Font font(SpreadsheetLayout.Cell cell) {
        int points = cell != null && cell.fontSize() > 0 ? cell.fontSize() : DEFAULT_FONT_POINTS;
        boolean bold = cell != null && cell.bold();
        return Font.font(FONT_FAMILY, bold ? FontWeight.BOLD : FontWeight.NORMAL, points * 4.0 / 3.0);
    }

    /** Taller for a larger font, as Excel grows a row to fit: 15 points for 11, about 19 for 14. */
    private static double rowHeight(SpreadsheetLayout layout, int row, int lastColumn) {
        int points = DEFAULT_FONT_POINTS;
        for (int column = 0; column <= lastColumn; column++) {
            SpreadsheetLayout.Cell cell = layout.cell(row, column);
            if (cell != null && cell.fontSize() > points) {
                points = cell.fontSize();
            }
        }
        return Math.max(DEFAULT_ROW_HEIGHT, Math.ceil(points * 1.36 * 4.0 / 3.0));
    }

    /** Excel's conversion from its width unit to pixels. */
    private static double pixels(Double width) {
        double characters = width == null ? DEFAULT_COLUMN_WIDTH : width;
        return Math.floor(characters * DIGIT_WIDTH + CELL_PADDING);
    }

    private static ColumnConstraints fixedColumn(double width) {
        ColumnConstraints constraints = new ColumnConstraints(width, width, width);
        constraints.setHgrow(Priority.NEVER);
        return constraints;
    }

    private static RowConstraints fixedRow(double height) {
        RowConstraints constraints = new RowConstraints(height, height, height);
        constraints.setVgrow(Priority.NEVER);
        return constraints;
    }

    private static String columnLetter(int zeroBasedIndex) {
        StringBuilder letters = new StringBuilder();
        int index = zeroBasedIndex + 1;
        while (index > 0) {
            int remainder = (index - 1) % 26;
            letters.insert(0, (char) ('A' + remainder));
            index = (index - 1) / 26;
        }
        return letters.toString();
    }
}
