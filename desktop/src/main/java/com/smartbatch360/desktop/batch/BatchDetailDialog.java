package com.smartbatch360.desktop.batch;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Read-only "Batch detail viewing" (docs/02_UI_REFERENCE.md's Batch Reports reference) - no editing, just review. */
public final class BatchDetailDialog {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());

    private BatchDetailDialog() {
    }

    public static void show(BatchDto batch) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch " + batch.batchNumber());
        dialog.setResizable(false);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(
                BatchDetailDialog.class.getResource("/css/theme.css").toExternalForm());

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
                batch.targetQuantity() + " / " + batch.producedQuantity() + " / " + batch.remainingQuantity() + " m³");
        row = addRow(grid, row, "Cycle Date/Time", batch.cycleDateTime() != null ? TIMESTAMP_FORMAT.format(batch.cycleDateTime()) : "");
        row = addRow(grid, row, "Cycle Number / Shift",
                (batch.cycleNumber() != null ? batch.cycleNumber().toString() : "-") + " / " + (batch.shift() != null ? batch.shift() : "-"));
        row = addRow(grid, row, "Status", batch.status().name());
        row = addRow(grid, row, "Equipment (Mixer/Conveyor/Water/Cement Screw/Compressor)",
                String.join(" / ", batch.mixerStatus().name(), batch.conveyorStatus().name(), batch.waterValveStatus().name(),
                        batch.cementScrewStatus().name(), batch.compressorStatus().name()));

        TableView<BatchMaterialDto> materialsTable = new TableView<>();
        materialsTable.setItems(javafx.collections.FXCollections.observableArrayList(batch.materials()));
        materialsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        materialsTable.setPrefHeight(160);
        materialsTable.setPrefWidth(480);

        TableColumn<BatchMaterialDto, String> nameCol = new TableColumn<>("Material");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().materialName()));
        TableColumn<BatchMaterialDto, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().target() + " " + cd.getValue().unit()));
        TableColumn<BatchMaterialDto, String> setpointCol = new TableColumn<>("Setpoint");
        setpointCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().setpoint() + " " + cd.getValue().unit()));
        TableColumn<BatchMaterialDto, String> achievedCol = new TableColumn<>("Achieved");
        achievedCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().achieved() + " " + cd.getValue().unit()));
        materialsTable.getColumns().setAll(List.of(nameCol, targetCol, setpointCol, achievedCol));

        Label materialsLabel = new Label("Material Consumption");
        materialsLabel.getStyleClass().add("page-header-subtitle");

        VBox content = new VBox(10, grid, materialsLabel, materialsTable);
        content.setPadding(new Insets(4));
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait();
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
