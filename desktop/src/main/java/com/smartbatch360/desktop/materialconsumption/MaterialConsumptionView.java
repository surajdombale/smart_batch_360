package com.smartbatch360.desktop.materialconsumption;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.common.PageHeader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Material Consumption (docs/02_UI_REFERENCE.md's "Material Consumption
 * reference"): target vs achieved vs variance/wastage, grouped by day/week/
 * month, built from existing Batch/BatchMaterial data. First-pass scope
 * confirmed with the user 2026-08-26: this table only - charts are a later
 * pass.
 */
public class MaterialConsumptionView {

    private final MaterialConsumptionApiClient apiClient = new MaterialConsumptionApiClient();

    private final BorderPane root = new BorderPane();
    private final NotificationBanner banner = new NotificationBanner();
    private final StackPane centerStack = new StackPane();
    private final TableView<MaterialConsumptionDto> table = new TableView<>();

    private final TextField materialNameField = new TextField();
    private final DatePicker dateFromField = new DatePicker();
    private final DatePicker dateToField = new DatePicker();
    private final ComboBox<MaterialConsumptionGroupBy> groupByField =
            new ComboBox<>(FXCollections.observableArrayList(MaterialConsumptionGroupBy.values()));

    public MaterialConsumptionView() {
        PageHeader header = new PageHeader("Material Consumption",
                "Target vs achieved material usage and variance, by day, week or month.");

        VBox top = new VBox(10, header, banner, buildFilterPanel());
        root.setTop(top);
        root.setCenter(centerStack);
        root.getStyleClass().add("content-area");

        setupTable();
        search();
    }

    private VBox buildFilterPanel() {
        materialNameField.setPromptText("e.g. Cement");
        groupByField.getSelectionModel().select(MaterialConsumptionGroupBy.DAY);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0,
                labeled("Material", materialNameField),
                labeled("Date From", dateFromField),
                labeled("Date To", dateToField),
                labeled("Group By", groupByField));

        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("button-primary");
        searchButton.setOnAction(e -> search());

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("button-secondary");
        resetButton.setOnAction(e -> resetFilters());

        HBox actions = new HBox(10, searchButton, resetButton);
        actions.setPadding(new Insets(8, 0, 0, 0));

        VBox panel = new VBox(8, grid, actions);
        panel.getStyleClass().add("card");
        return panel;
    }

    private VBox labeled(String label, javafx.scene.Node control) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        return new VBox(4, labelNode, control);
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No material consumption for these filters."));

        TableColumn<MaterialConsumptionDto, String> periodCol = new TableColumn<>("Period");
        periodCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().period()));

        TableColumn<MaterialConsumptionDto, String> materialCol = new TableColumn<>("Material");
        materialCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().materialName()));

        TableColumn<MaterialConsumptionDto, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(cd -> new SimpleStringProperty(
                formatQuantity(cd.getValue().totalTarget(), cd.getValue().unit())));

        TableColumn<MaterialConsumptionDto, String> achievedCol = new TableColumn<>("Achieved");
        achievedCol.setCellValueFactory(cd -> new SimpleStringProperty(
                formatQuantity(cd.getValue().totalAchieved(), cd.getValue().unit())));

        TableColumn<MaterialConsumptionDto, MaterialConsumptionDto> varianceCol = new TableColumn<>("Variance");
        varianceCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        varianceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(MaterialConsumptionDto row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().removeAll("status-up", "status-down");
                if (empty || row == null) {
                    setText(null);
                    return;
                }
                BigDecimal variance = row.variance();
                setText(formatSignedQuantity(variance, row.unit()));
                if (variance.compareTo(BigDecimal.ZERO) > 0) {
                    getStyleClass().add("status-down"); // over-consumption/wastage
                } else if (variance.compareTo(BigDecimal.ZERO) < 0) {
                    getStyleClass().add("status-up"); // under-consumption/savings
                }
            }
        });

        TableColumn<MaterialConsumptionDto, String> batchesCol = new TableColumn<>("Batches");
        batchesCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().batchCount())));

        table.getColumns().setAll(List.of(periodCol, materialCol, targetCol, achievedCol, varianceCol, batchesCol));
    }

    private String formatQuantity(BigDecimal value, String unit) {
        return value == null ? "" : value.toPlainString() + (unit != null ? " " + unit : "");
    }

    private String formatSignedQuantity(BigDecimal value, String unit) {
        if (value == null) {
            return "";
        }
        String sign = value.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return sign + value.toPlainString() + (unit != null ? " " + unit : "");
    }

    private void resetFilters() {
        materialNameField.clear();
        dateFromField.setValue(null);
        dateToField.setValue(null);
        groupByField.getSelectionModel().select(MaterialConsumptionGroupBy.DAY);
        search();
    }

    private void search() {
        centerStack.getChildren().setAll(loadingIndicator());

        String materialName = materialNameField.getText() == null || materialNameField.getText().isBlank()
                ? null : materialNameField.getText().trim();

        apiClient.search(materialName, dateFromField.getValue(), dateToField.getValue(),
                        groupByField.getValue())
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        showError(throwable);
                    } else {
                        showResults(result);
                    }
                }));
    }

    private ProgressIndicator loadingIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(48, 48);
        return indicator;
    }

    private void showError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause instanceof ApiException apiEx ? apiEx.getMessage() : "Something went wrong. Please try again.";

        VBox box = new VBox(8);
        box.getStyleClass().addAll("state-container", "state-error");
        box.setAlignment(Pos.CENTER);
        Label title = new Label("Unable to load material consumption");
        title.getStyleClass().add("state-title");
        Label msg = new Label(message);
        msg.getStyleClass().add("state-message");
        msg.setWrapText(true);
        Button retry = new Button("Retry");
        retry.getStyleClass().add("button-secondary");
        retry.setOnAction(e -> search());
        box.getChildren().addAll(title, msg, retry);
        centerStack.getChildren().setAll(box);
    }

    private void showResults(List<MaterialConsumptionDto> result) {
        table.setItems(FXCollections.observableArrayList(result));
        centerStack.getChildren().setAll(table);
    }

    public Region getView() {
        return root;
    }
}
