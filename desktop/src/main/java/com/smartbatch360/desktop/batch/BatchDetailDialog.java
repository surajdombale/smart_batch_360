package com.smartbatch360.desktop.batch;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Read-only "Batch detail viewing" (docs/02_UI_REFERENCE.md's Batch Reports reference) - no editing, just review.
 *
 * Carries the same cycle grid as the printed batch report, worked out by the
 * same BatchSheet, so what is on screen and what is printed cannot disagree.
 */
public final class BatchDetailDialog {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());

    private static final double GRID_ROW_HEIGHT = 26;
    private static final int GRID_VISIBLE_ROWS = 14;
    private static final double GRID_MAX_WIDTH = 1100;
    private static final double SCROLLBAR = 16;

    private BatchDetailDialog() {
    }

    public static void show(BatchDto batch, List<BatchCycleDto> cycles) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch " + batch.batchNumber());
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
                BatchDetailDialog.class.getResource("/css/theme.css").toExternalForm());
        dialog.getDialogPane().setContent(content(batch, cycles));

        dialog.showAndWait();
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
        materialsTable.setPrefHeight(160);
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
        // Up to twenty materials fit across the sheet, so the grid scrolls
        // sideways rather than squeezing every figure into an ellipsis.
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

        // Room for the header and every line up to a screenful, then it scrolls -
        // with space kept for a scrollbar only where one will appear.
        boolean scrollsDown = rows.size() > GRID_VISIBLE_ROWS;
        boolean scrollsAcross = width + SCROLLBAR > GRID_MAX_WIDTH;
        table.setPrefHeight(GRID_ROW_HEIGHT * (Math.min(rows.size(), GRID_VISIBLE_ROWS) + 1) + 4
                + (scrollsAcross ? SCROLLBAR : 0));
        table.setPrefWidth(Math.min(width + 4 + (scrollsDown ? SCROLLBAR : 0), GRID_MAX_WIDTH));
        return table;
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
