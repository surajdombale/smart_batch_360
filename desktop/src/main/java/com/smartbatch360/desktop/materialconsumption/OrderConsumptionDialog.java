package com.smartbatch360.desktop.materialconsumption;

import com.smartbatch360.desktop.api.ApiException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

/**
 * Read-only view of how much of each material an order consumes, following
 * Order -> Recipe -> Recipe Materials -> Material. The numbers come from the
 * backend (which scales the recipe to the ordered volume); nothing is
 * calculated here.
 */
public final class OrderConsumptionDialog {

    private OrderConsumptionDialog() {
    }

    public static void show(Long orderId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Order #" + orderId + " - Material Consumption");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                OrderConsumptionDialog.class.getResource("/css/theme.css")).toExternalForm());

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.setPrefWidth(520);

        ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxSize(48, 48);
        content.getChildren().add(loading);
        dialog.getDialogPane().setContent(content);

        new MaterialConsumptionApiClient().forOrder(orderId).whenComplete((result, throwable) ->
                Platform.runLater(() -> {
                    content.getChildren().clear();
                    if (throwable != null) {
                        content.getChildren().add(errorLabel(throwable));
                    } else {
                        content.getChildren().addAll(summaryGrid(result), materialsTable(result.materials()));
                    }
                }));

        dialog.showAndWait();
    }

    private static Label errorLabel(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause instanceof ApiException apiEx
                ? apiEx.getMessage()
                : "Something went wrong. Please try again.";
        Label label = new Label(message);
        label.getStyleClass().add("state-message");
        label.setWrapText(true);
        return label;
    }

    private static GridPane summaryGrid(OrderConsumptionDto dto) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        int row = 0;
        addRow(grid, row++, "Customer", dto.clientName());
        addRow(grid, row++, "Site", dto.siteName());
        addRow(grid, row++, "Recipe", dto.recipeName());
        addRow(grid, row++, "Order Quantity", dto.orderQuantityM3().toPlainString() + " m³");
        addRow(grid, row, "Recipe Batch Quantity", dto.recipeBatchQuantityM3().toPlainString() + " m³");
        return grid;
    }

    private static void addRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        grid.add(labelNode, 0, row);
        grid.add(new Label(value), 1, row);
    }

    private static TableView<OrderMaterialConsumptionDto> materialsTable(List<OrderMaterialConsumptionDto> materials) {
        TableView<OrderMaterialConsumptionDto> table =
                new TableView<>(FXCollections.observableArrayList(materials));
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(220);
        table.setPlaceholder(new Label("This recipe has no materials."));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<OrderMaterialConsumptionDto, String> nameCol = new TableColumn<>("Material");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().materialName()));

        TableColumn<OrderMaterialConsumptionDto, String> quantityCol = new TableColumn<>("Consumption");
        quantityCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().quantity().toPlainString() + " " + cd.getValue().unit()));

        table.getColumns().setAll(List.of(nameCol, quantityCol));
        return table;
    }
}
