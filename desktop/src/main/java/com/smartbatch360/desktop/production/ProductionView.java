package com.smartbatch360.desktop.production;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.batch.BatchDto;
import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.common.PageHeader;
import com.smartbatch360.desktop.order.OrderApiClient;
import com.smartbatch360.desktop.order.OrderDto;
import com.smartbatch360.desktop.vehicle.VehicleApiClient;
import com.smartbatch360.desktop.vehicle.VehicleDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

/**
 * Loading a batch, as the plant describes it: pick the order, pick the vehicle,
 * type how many cubic metres, and start.
 *
 * Only the batch size is typed. The number of cycles and the quantity per cycle
 * follow from it and the plant's mixer capacity, and the material setpoints
 * follow from the order's recipe - all calculated by the backend as the size is
 * typed, so what the screen shows is what starting production will actually do.
 *
 * The customer, site and recipe are shown but never chosen here: they belong to
 * the order, and a batch that disagrees with the order it fulfils is what the
 * batch-to-order guards exist to prevent.
 */
public class ProductionView {

    private final ProductionApiClient apiClient = new ProductionApiClient();
    private final OrderApiClient orderApiClient = new OrderApiClient();
    private final VehicleApiClient vehicleApiClient = new VehicleApiClient();

    private final BorderPane root = new BorderPane();
    private final NotificationBanner banner = new NotificationBanner();

    private final ComboBox<OrderDto> orderField = new ComboBox<>();
    private final ComboBox<VehicleDto> vehicleField = new ComboBox<>();
    private final TextField batchSizeField = new TextField();
    private final TextField batchNumberField = new TextField();
    private final TextField shiftField = new TextField();

    private final Label customerValue = new Label("-");
    private final Label siteValue = new Label("-");
    private final Label recipeValue = new Label("-");
    private final Label remainingValue = new Label("-");
    private final Label cyclesValue = new Label("-");
    private final Label perCycleValue = new Label("-");
    private final Label totalValue = new Label("-");
    private final Label mixerValue = new Label("-");

    private final TableView<ProductionPlanDto.MaterialSetpointDto> setpointsTable = new TableView<>();
    private final Button startButton = new Button("Start Production");

    private ProductionPlanDto plan;

    public ProductionView() {
        PageHeader header = new PageHeader("Production",
                "Load a batch against an order: the cycles and per-cycle quantity are worked out for you.");

        VBox content = new VBox(12, buildSelectionCard(), buildSizeCard(), buildSetpointsCard(), buildActions());
        content.setPadding(new Insets(0, 0, 12, 0));

        VBox top = new VBox(10, header, banner);
        root.setTop(top);
        root.setCenter(content);
        root.getStyleClass().add("content-area");

        setupTable();
        loadReferenceLists();

        orderField.valueProperty().addListener((obs, old, order) -> onOrderChanged(order));
        batchSizeField.textProperty().addListener((obs, old, text) -> refreshPlan());
        startButton.setOnAction(e -> startProduction());
        startButton.setDisable(true);
    }

    private VBox buildSelectionCard() {
        orderField.setPromptText("Select an order");
        vehicleField.setPromptText("Select a vehicle");
        orderField.setMaxWidth(Double.MAX_VALUE);
        vehicleField.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.addRow(0, labelled("Order", orderField), labelled("Vehicle", vehicleField));
        grid.addRow(1, readOnly("Customer", customerValue), readOnly("Site", siteValue),
                readOnly("Recipe", recipeValue), readOnly("Order remaining", remainingValue));

        VBox card = new VBox(8, sectionTitle("1. Select the order and vehicle"), grid);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildSizeCard() {
        batchSizeField.setPromptText("e.g. 6 (minimum 0.1)");
        batchNumberField.setPromptText("left blank, the plant numbers it");
        shiftField.setPromptText("e.g. Day");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.addRow(0, labelled("Batch Size (m3)", batchSizeField),
                readOnly("Number of Cycles", cyclesValue),
                readOnly("Per Cycle Quantity", perCycleValue));
        grid.addRow(1, readOnly("Mixer capacity", mixerValue), readOnly("Load weight", totalValue),
                labelled("Batch Number", batchNumberField), labelled("Shift", shiftField));

        VBox card = new VBox(8, sectionTitle("2. Enter the batch size - the rest is calculated"), grid);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildSetpointsCard() {
        setpointsTable.setPrefHeight(220);
        VBox card = new VBox(8, sectionTitle("3. Material setpoints, adjusted to the per-cycle quantity"),
                setpointsTable);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox buildActions() {
        startButton.getStyleClass().add("button-primary");
        startButton.setMinWidth(Region.USE_PREF_SIZE);
        HBox actions = new HBox(10, startButton);
        actions.setPadding(new Insets(4, 0, 0, 0));
        return actions;
    }

    private void setupTable() {
        TableColumn<ProductionPlanDto.MaterialSetpointDto, String> nameCol = new TableColumn<>("Material");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().materialName()));

        TableColumn<ProductionPlanDto.MaterialSetpointDto, String> perCycleCol =
                new TableColumn<>("Setpoint per Cycle (kg)");
        perCycleCol.setCellValueFactory(cd -> new SimpleStringProperty(trim(cd.getValue().perCycleKg())));

        TableColumn<ProductionPlanDto.MaterialSetpointDto, String> totalCol = new TableColumn<>("Whole Load (kg)");
        totalCol.setCellValueFactory(cd -> new SimpleStringProperty(trim(cd.getValue().totalKg())));

        setpointsTable.getColumns().setAll(List.of(nameCol, perCycleCol, totalCol));
        setpointsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setpointsTable.setPlaceholder(new Label("Choose an order and enter a batch size."));
    }

    private void loadReferenceLists() {
        orderApiClient.list().whenComplete((orders, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                banner.showError("Orders could not be loaded: " + message(throwable));
                return;
            }
            orderField.setItems(FXCollections.observableArrayList(orders));
            if (orders.isEmpty()) {
                banner.showError("There are no orders yet. Add one before starting production.");
            }
        }));

        vehicleApiClient.list().whenComplete((vehicles, throwable) -> Platform.runLater(() -> {
            if (throwable == null) {
                vehicleField.setItems(FXCollections.observableArrayList(vehicles));
            }
        }));
    }

    private void onOrderChanged(OrderDto order) {
        customerValue.setText(order == null ? "-" : order.clientName());
        siteValue.setText(order == null ? "-" : order.siteName());
        recipeValue.setText(order == null ? "-" : order.recipeName());
        remainingValue.setText(order == null ? "-" : trim(order.remainingQuantityKg()) + " kg");
        refreshPlan();
    }

    /**
     * Asks the backend to work the load out. Done there rather than here so the
     * figures on screen are the ones production will actually use - a second
     * implementation in the UI is a second thing to get wrong.
     */
    private void refreshPlan() {
        OrderDto order = orderField.getValue();
        BigDecimal size = parseSize(batchSizeField.getText());
        if (order == null || size == null) {
            clearPlan();
            return;
        }

        apiClient.plan(new ProductionPlanRequestDto(order.id(), size))
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        clearPlan();
                        banner.showError(message(throwable));
                        return;
                    }
                    banner.hide();
                    plan = result;
                    cyclesValue.setText(String.valueOf(result.cycles()));
                    perCycleValue.setText(trim(result.perCycleM3()) + " m3  (" + trim(result.perCycleKg()) + " kg)");
                    totalValue.setText(trim(result.totalKg()) + " kg");
                    mixerValue.setText(trim(result.mixerCapacityM3()) + " m3");
                    remainingValue.setText(trim(result.orderRemainingKg()) + " kg");
                    setpointsTable.setItems(FXCollections.observableArrayList(result.materials()));
                    startButton.setDisable(false);
                }));
    }

    private void clearPlan() {
        plan = null;
        cyclesValue.setText("-");
        perCycleValue.setText("-");
        totalValue.setText("-");
        setpointsTable.getItems().clear();
        startButton.setDisable(true);
    }

    private void startProduction() {
        OrderDto order = orderField.getValue();
        VehicleDto vehicle = vehicleField.getValue();
        BigDecimal size = parseSize(batchSizeField.getText());
        if (order == null || size == null) {
            banner.showError("Choose an order and enter a batch size.");
            return;
        }
        if (vehicle == null) {
            banner.showError("Choose the vehicle this load is going on.");
            return;
        }

        startButton.setDisable(true);
        apiClient.start(new StartProductionRequestDto(order.id(), vehicle.id(), size,
                        batchNumberField.getText(), shiftField.getText()))
                .whenComplete((batch, throwable) -> Platform.runLater(() -> {
                    startButton.setDisable(false);
                    if (throwable != null) {
                        banner.showError(message(throwable));
                    } else {
                        onStarted(batch);
                    }
                }));
    }

    private void onStarted(BatchDto batch) {
        banner.showSuccess("Batch " + batch.batchNumber() + " started: " + plan.cycles() + " cycles of "
                + trim(plan.perCycleM3()) + " m3. The PLC reports each cycle as it runs.");
        batchNumberField.clear();
        batchSizeField.clear();
        clearPlan();
        // The order's remaining quantity has moved on now there is a batch against it.
        loadReferenceLists();
        orderField.getSelectionModel().clearSelection();
    }

    private BigDecimal parseSize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            return value.compareTo(new BigDecimal("0.1")) >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private VBox labelled(String label, javafx.scene.Node control) {
        Label caption = new Label(label);
        caption.getStyleClass().add("form-label");
        VBox box = new VBox(4, caption, control);
        box.setMinWidth(170);
        return box;
    }

    private VBox readOnly(String label, Label value) {
        Label caption = new Label(label);
        caption.getStyleClass().add("form-label");
        value.getStyleClass().add("line-total-value");
        VBox box = new VBox(4, caption, value);
        box.setMinWidth(170);
        return box;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private String trim(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String message(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof ApiException apiEx) {
            return apiEx.getMessage();
        }
        return "Something went wrong. Please try again.";
    }

    public BorderPane getView() {
        return root;
    }
}
