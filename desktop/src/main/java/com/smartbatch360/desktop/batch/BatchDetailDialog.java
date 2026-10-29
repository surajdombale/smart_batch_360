package com.smartbatch360.desktop.batch;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Screen;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Read-only "Batch detail viewing" (docs/02_UI_REFERENCE.md's Batch Reports reference) - no editing, just review.
 *
 * Carries the same cycle grid as the printed batch report, worked out by the
 * same BatchSheet, so what is on screen and what is printed cannot disagree.
 *
 * Sized to the screen and scrolled as one, down and across. A batch of 27
 * cycles made the dialog taller than the screen, with its foot and Close
 * button out of reach (28-Oct-2026), and twenty materials would run past its
 * side. Both tables now show every row and the grid every column, so the
 * dialog has one pair of scrollbars rather than tables scrolling inside a
 * scrolling page.
 */
public final class BatchDetailDialog {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());

    private static final double GRID_ROW_HEIGHT = 26;
    private static final double SCROLLBAR = 16;
    /** The most of the screen the dialog takes before its content scrolls. */
    private static final double MAX_SCREEN_WIDTH = 0.9;
    private static final double MAX_SCREEN_HEIGHT = 0.8;

    private BatchDetailDialog() {
    }

    public static void show(BatchDto batch, List<BatchCycleDto> cycles) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch " + batch.batchNumber());
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
                BatchDetailDialog.class.getResource("/css/theme.css").toExternalForm());
        dialog.getDialogPane().setContent(scrolling(content(batch, cycles)));

        dialog.showAndWait();
    }

    /**
     * The content in a scroll pane as large as it needs, up to most of the
     * screen. Measured in a scene of its own first, with the theme applied,
     * because a node's preferred size means nothing until its styles are known.
     */
    static ScrollPane scrolling(VBox content) {
        Scene measuring = new Scene(new Group(content));
        measuring.getStylesheets().add(BatchDetailDialog.class.getResource("/css/theme.css").toExternalForm());
        content.applyCss();
        double width = content.prefWidth(-1);
        double height = content.prefHeight(width);

        ScrollPane scroller = new ScrollPane(content);
        scroller.getStyleClass().add("dialog-scroll");
        // Stretched to the dialog's width when there is room, never narrower than
        // the cycle grid - which is held at its full width, so too many materials
        // scroll the dialog sideways instead of squeezing the grid.
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double maxWidth = screen.getWidth() * MAX_SCREEN_WIDTH;
        double maxHeight = screen.getHeight() * MAX_SCREEN_HEIGHT;
        // Room beside and below the content for the scrollbars it will need when cut short.
        scroller.setPrefViewportWidth(Math.min(width + (height > maxHeight ? SCROLLBAR : 0), maxWidth));
        scroller.setPrefViewportHeight(Math.min(height + (width > maxWidth ? SCROLLBAR : 0), maxHeight));
        return scroller;
    }

    static VBox content(BatchDto batch, List<BatchCycleDto> cycles) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        int row = 0;
        row = addRow(grid, row, "Batch Number", batch.batchNumber());
        row = addRow(grid, row, "Recipe", batch.recipeName());
        row = addRow(grid, row, "Client", batch.clientName());
        row = addRow(grid, row, "Site", batch.siteName());
        row = addRow(grid, row, "Vehicle", batch.vehicleNumber());
        row = addRow(grid, row, "Driver", batch.driverName());
        row = addRow(grid, row, "Target / Produced / Remaining",
                batch.targetQuantity() + " / " + batch.producedQuantity() + " / " + batch.remainingQuantity() + " kg");
        row = addRow(grid, row, "Cycle Date/Time", batch.cycleDateTime() != null ? TIMESTAMP_FORMAT.format(batch.cycleDateTime()) : "");
        row = addRow(grid, row, "Cycle Number / Shift",
                (batch.cycleNumber() != null ? batch.cycleNumber().toString() : "-") + " / " + (batch.shift() != null ? batch.shift() : "-"));
        row = addRow(grid, row, "Status", batch.status().name());

        TableView<BatchMaterialDto> materialsTable = new TableView<>();
        materialsTable.setItems(FXCollections.observableArrayList(batch.materials()));
        materialsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Every material shown; the dialog scrolls, not the table.
        materialsTable.setFixedCellSize(GRID_ROW_HEIGHT);
        fitRows(materialsTable, batch.materials() == null ? 0 : batch.materials().size(), 0);
        materialsTable.setPrefWidth(480);

        TableColumn<BatchMaterialDto, String> nameCol = new TableColumn<>("Material");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().materialName()));
        TableColumn<BatchMaterialDto, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().target() + " " + cd.getValue().unit()));
        TableColumn<BatchMaterialDto, String> setpointCol = new TableColumn<>("Setpoint");
        setpointCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().setpoint() + " " + cd.getValue().unit()));
        TableColumn<BatchMaterialDto, String> achievedCol = new TableColumn<>("Achieved");
        achievedCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().achieved() + " " + cd.getValue().unit()));
        materialsTable.getColumns().setAll(List.of(nameCol, targetCol, setpointCol, achievedCol));

        Label materialsLabel = new Label("Material Consumption");
        materialsLabel.getStyleClass().add("page-header-subtitle");

        BatchSheet sheet = BatchSheet.of(batch, cycles);
        Label cyclesLabel = new Label("Cycles (kg)");
        cyclesLabel.getStyleClass().add("page-header-subtitle");

        VBox content = new VBox(10, grid, materialsLabel, materialsTable, cyclesLabel, cycleGrid(sheet));
        if (!sheet.hasCycles()) {
            content.getChildren().add(new Label("No cycles have been recorded for this batch yet."));
        }
        content.getChildren().add(new Label("Actual Produced Quantity : " + sheet.producedTonnes() + " TON"));
        content.setPadding(new Insets(4));
        return content;
    }

    /** Cycles down, materials across - the printed sheet's grid, line for line. */
    private static TableView<BatchSheet.Row> cycleGrid(BatchSheet sheet) {
        List<BatchSheet.Row> rows = sheet.body();
        TableView<BatchSheet.Row> table = new TableView<>(FXCollections.observableArrayList(rows));
        table.setFixedCellSize(GRID_ROW_HEIGHT);
        // Columns keep their own widths rather than sharing the grid's, so up to
        // twenty materials never squeeze a figure into an ellipsis.
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        List<String> headings = sheet.headings().cells();
        double width = 0;
        for (int i = 0; i < headings.size(); i++) {
            int column = i;
            TableColumn<BatchSheet.Row, String> col = new TableColumn<>(headings.get(i));
            col.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().cells().get(column)));
            col.setSortable(false);
            col.setReorderable(false);
            // Wide enough for its heading, so a long material name is not cut short.
            double headingWidth = new Text(headings.get(i)).getLayoutBounds().getWidth() + 30;
            col.setPrefWidth(Math.max(i == 0 ? 78 : i == 1 ? 72 : 82, headingWidth));
            if (i > 1) {
                col.setStyle("-fx-alignment: CENTER-RIGHT;");
            }
            width += col.getPrefWidth();
            table.getColumns().add(col);
        }

        table.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(BatchSheet.Row row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("batch-sheet-band");
                if (!empty && row != null && row.shaded()) {
                    getStyleClass().add("batch-sheet-band");
                }
            }
        });

        // Every line and every column shown, the grid held at that size: the
        // dialog scrolls both ways, so its scrollbars sit at its own edges
        // rather than under the last of 27 cycles.
        fitRows(table, rows.size(), 0);
        table.setPrefWidth(width + 4);
        table.setMinWidth(width + 4);
        table.setMaxWidth(width + 4);
        return table;
    }

    /** Tall enough for its heading and every row, and held there so the dialog cannot squeeze it. */
    private static void fitRows(TableView<?> table, int rows, double extra) {
        double height = (GRID_ROW_HEIGHT * (Math.max(rows, 1) + 1)) + 4 + extra;
        table.setPrefHeight(height);
        table.setMinHeight(height);
        table.setMaxHeight(height);
    }

    private static int addRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        Label valueNode = new Label(value == null ? "" : value);
        valueNode.setWrapText(true);
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
        return row + 1;
    }
}
