package com.smartbatch360.desktop.production;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.batch.BatchCycleDto;
import com.smartbatch360.desktop.batch.BatchDto;
import com.smartbatch360.desktop.batch.BatchMaterialDto;
import com.smartbatch360.desktop.batch.BatchStatus;
import com.smartbatch360.desktop.common.ConfirmDialogs;
import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.driver.DriverApiClient;
import com.smartbatch360.desktop.driver.DriverDto;
import com.smartbatch360.desktop.common.PageHeader;
import com.smartbatch360.desktop.order.OrderApiClient;
import com.smartbatch360.desktop.order.OrderDto;
import com.smartbatch360.desktop.vehicle.VehicleApiClient;
import com.smartbatch360.desktop.vehicle.VehicleDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Loading a batch, as the plant describes it: pick the order, pick the vehicle,
 * type how many cubic metres, and start.
 *
 * The batch number is not entered here: the PLC generates it. Nor is the
 * shift. Only the batch size is typed - the number of cycles and the quantity per cycle
 * follow from it and the plant's mixer capacity, and the material setpoints
 * follow from the order's recipe - all calculated by the backend as the size is
 * typed, so what the screen shows is what starting production will actually do.
 *
 * The customer, site and recipe are shown but never chosen here: they belong to
 * the order, and a batch that disagrees with the order it fulfils is what the
 * batch-to-order guards exist to prevent.
 *
 * While a batch is running the screen shows that batch instead, with its next
 * cycle to type in - the plant runs one batch at a time, so there is nothing
 * to load until it is done. That entry stands in for the PLC until it is
 * connected: each cycle goes through the endpoint the PLC uses, and the last
 * planned cycle completes the batch.
 */
public class ProductionView {

    private final ProductionApiClient apiClient = new ProductionApiClient();
    private final OrderApiClient orderApiClient = new OrderApiClient();
    private final VehicleApiClient vehicleApiClient = new VehicleApiClient();
    private final DriverApiClient driverApiClient = new DriverApiClient();

    private final BorderPane root = new BorderPane();
    private final NotificationBanner banner = new NotificationBanner();

    private final ComboBox<OrderDto> orderField = new ComboBox<>();
    private final ComboBox<VehicleDto> vehicleField = new ComboBox<>();
    private final ComboBox<DriverDto> driverField = new ComboBox<>();
    private final TextField batchSizeField = new TextField();

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

    private final VBox setupContent;
    private final VBox runningContent;
    private final Label runningSummary = new Label();
    private final Label cycleProgress = new Label();
    private final GridPane cycleEntry = new GridPane();
    private final List<TextField> weighedFields = new ArrayList<>();
    private final Button recordCycleButton = new Button();
    private final Button stopBatchButton = new Button("Stop Batch");

    /** The batch on screen while one is running, and the number its next cycle will carry. */
    private BatchDto runningBatch;
    private int nextCycle;

    private record Running(BatchDto batch, List<BatchCycleDto> cycles) {
    }

    public ProductionView() {
        PageHeader header = new PageHeader("Production",
                "Load a batch against an order: the cycles and per-cycle quantity are worked out for you.");

        setupContent = new VBox(12, buildSelectionCard(), buildSizeCard(), buildSetpointsCard(), buildActions());
        setupContent.setPadding(new Insets(0, 0, 12, 0));
        runningContent = new VBox(12, buildRunningCard());
        runningContent.setPadding(new Insets(0, 0, 12, 0));

        VBox top = new VBox(10, header, banner);
        root.setTop(top);
        root.setCenter(setupContent);
        root.getStyleClass().add("content-area");

        setupTable();
        loadReferenceLists();
        loadRunningBatch();

        orderField.valueProperty().addListener((obs, old, order) -> onOrderChanged(order));
        batchSizeField.textProperty().addListener((obs, old, text) -> refreshPlan());
        startButton.setOnAction(e -> startProduction());
        startButton.setDisable(true);
        recordCycleButton.setOnAction(e -> recordCycle());
        stopBatchButton.setOnAction(e -> stopBatch());
    }

    private VBox buildSelectionCard() {
        orderField.setPromptText("Select an order");
        vehicleField.setPromptText("Select a vehicle");
        driverField.setPromptText("Select a driver");
        orderField.setMaxWidth(Double.MAX_VALUE);
        vehicleField.setMaxWidth(Double.MAX_VALUE);
        driverField.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.addRow(0, labelled("Order", orderField), labelled("Vehicle", vehicleField),
                labelled("Driver", driverField));
        grid.addRow(1, readOnly("Customer", customerValue), readOnly("Site", siteValue),
                readOnly("Recipe", recipeValue), readOnly("Order remaining", remainingValue));

        VBox card = new VBox(8, sectionTitle("1. Select the order, vehicle and driver"), grid);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildSizeCard() {
        batchSizeField.setPromptText("e.g. 6 (minimum 0.1)");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.addRow(0, labelled("Batch Size (m3)", batchSizeField),
                readOnly("Number of Cycles", cyclesValue),
                readOnly("Per Cycle Quantity", perCycleValue));
        grid.addRow(1, readOnly("Mixer capacity", mixerValue), readOnly("Load weight", totalValue));

        VBox card = new VBox(8, sectionTitle("2. Enter the batch size - the rest is calculated"), grid);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildSetpointsCard() {
        setpointsTable.setPrefHeight(220);
        VBox card = new VBox(8,
                sectionTitle("3. Targets from the recipe, and the setpoints this load works out to"),
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

    private VBox buildRunningCard() {
        cycleEntry.setHgap(24);
        cycleEntry.setVgap(6);
        runningSummary.getStyleClass().add("line-total-value");
        recordCycleButton.getStyleClass().add("button-primary");
        recordCycleButton.setMinWidth(Region.USE_PREF_SIZE);
        stopBatchButton.getStyleClass().add("button-danger");
        stopBatchButton.setMinWidth(Region.USE_PREF_SIZE);

        Label hint = new Label("Until the PLC is connected, type what each material weighed in this cycle - "
                + "it starts at the setpoint. The last planned cycle completes the batch.");
        hint.setWrapText(true);
        hint.getStyleClass().add("page-header-subtitle");

        HBox actions = new HBox(10, recordCycleButton, stopBatchButton);
        actions.setPadding(new Insets(4, 0, 0, 0));

        VBox card = new VBox(8, sectionTitle("Running batch - record each cycle as it is weighed"),
                runningSummary, cycleProgress, hint, cycleEntry, actions);
        card.getStyleClass().add("card");
        return card;
    }

    private void setupTable() {
        TableColumn<ProductionPlanDto.MaterialSetpointDto, String> nameCol = new TableColumn<>("Material");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().materialName()));

        TableColumn<ProductionPlanDto.MaterialSetpointDto, String> targetCol =
                new TableColumn<>("Target - from recipe (kg)");
        targetCol.setCellValueFactory(cd -> new SimpleStringProperty(trim(cd.getValue().recipeQuantityKg())));

        TableColumn<ProductionPlanDto.MaterialSetpointDto, String> perCycleCol =
                new TableColumn<>("Setpoint per Cycle (kg)");
        perCycleCol.setCellValueFactory(cd -> new SimpleStringProperty(trim(cd.getValue().perCycleKg())));

        TableColumn<ProductionPlanDto.MaterialSetpointDto, String> totalCol = new TableColumn<>("Whole Load (kg)");
        totalCol.setCellValueFactory(cd -> new SimpleStringProperty(trim(cd.getValue().totalKg())));

        setpointsTable.getColumns().setAll(List.of(nameCol, targetCol, perCycleCol, totalCol));
        setpointsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setpointsTable.setPlaceholder(new Label("Choose an order and enter a batch size."));
    }

    private void loadReferenceLists() {
        orderApiClient.list().whenComplete((orders, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                banner.showError("Orders could not be loaded: " + message(throwable));
                return;
            }
            // Only orders that can still be produced against. A fulfilled or
            // cancelled one would only be refused once picked.
            List<OrderDto> open = orders.stream().filter(o -> o.status() != null && o.status().isOpen()).toList();
            orderField.setItems(FXCollections.observableArrayList(open));
            if (open.isEmpty()) {
                banner.showError(orders.isEmpty()
                        ? "There are no orders yet. Add one before starting production."
                        : "Every order is fulfilled or cancelled. Add a new one before starting production.");
            }
        }));

        vehicleApiClient.list().whenComplete((vehicles, throwable) -> Platform.runLater(() -> {
            if (throwable == null) {
                vehicleField.setItems(FXCollections.observableArrayList(vehicles));
            }
        }));

        driverApiClient.list().whenComplete((drivers, throwable) -> Platform.runLater(() -> {
            if (throwable == null) {
                driverField.setItems(FXCollections.observableArrayList(drivers));
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
        DriverDto driver = driverField.getValue();
        apiClient.start(new StartProductionRequestDto(order.id(), vehicle.id(), size, null, null, null,
                        driver == null ? null : driver.id()))
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
                + trim(plan.perCycleM3()) + " m3. Record each cycle as it is weighed.");
        batchSizeField.clear();
        clearPlan();
        // The order's remaining quantity and status have moved on now there is
        // a batch against it.
        loadReferenceLists();
        orderField.getSelectionModel().clearSelection();
        loadRunningBatch();
    }

    // ------------------------------------------------------------ running batch

    /** Shows the running batch if there is one, and otherwise the screen for loading the next. */
    private void loadRunningBatch() {
        apiClient.running()
                .thenCompose(batches -> batches.isEmpty()
                        ? CompletableFuture.completedFuture((Running) null)
                        : apiClient.cycles(batches.get(0).id()).thenApply(cycles -> new Running(batches.get(0), cycles)))
                .whenComplete((running, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        banner.showError("The running batch could not be loaded: " + message(throwable));
                        showSetup();
                    } else if (running == null) {
                        showSetup();
                    } else {
                        showRunning(running.batch(), running.cycles());
                    }
                }));
    }

    private void showSetup() {
        runningBatch = null;
        root.setCenter(setupContent);
    }

    private void showRunning(BatchDto batch, List<BatchCycleDto> cycles) {
        runningBatch = batch;
        nextCycle = cycles.stream().map(BatchCycleDto::cycleNumber).filter(Objects::nonNull)
                .max(Integer::compare).orElse(0) + 1;
        Integer planned = batch.cycleNumber();

        runningSummary.setText("Batch " + batch.batchNumber() + "  -  Order #" + batch.orderId() + ", "
                + batch.recipeName() + ", " + trim(batch.batchSizeM3()) + " m3 on " + batch.vehicleNumber());
        cycleProgress.setText(cycles.size() + (planned == null ? "" : " of " + planned) + " cycles recorded  -  "
                + trim(batch.producedQuantity()) + " of " + trim(batch.targetQuantity()) + " kg produced so far");

        cycleEntry.getChildren().clear();
        weighedFields.clear();
        cycleEntry.addRow(0, columnHeading("Material"), columnHeading("Setpoint per cycle (kg)"),
                columnHeading("Weighed in cycle " + nextCycle + " (kg)"));
        int row = 1;
        for (BatchMaterialDto material : batch.materials()) {
            TextField weighed = new TextField(material.setpoint() == null ? "" : material.setpoint().toPlainString());
            weighed.setPrefColumnCount(9);
            weighedFields.add(weighed);
            cycleEntry.addRow(row++, new Label(material.materialName()), new Label(trim(material.setpoint())), weighed);
        }

        if (planned == null) {
            recordCycleButton.setText("Record Cycle " + nextCycle);
        } else if (nextCycle >= planned) {
            recordCycleButton.setText("Record Last Cycle (" + nextCycle + " of " + planned + ")");
        } else {
            recordCycleButton.setText("Record Cycle " + nextCycle + " of " + planned);
        }
        setRunningControlsDisabled(false);
        root.setCenter(runningContent);
    }

    private void recordCycle() {
        BatchDto batch = runningBatch;
        List<BatchCycleRequestDto.Material> materials = new ArrayList<>();
        for (int i = 0; i < batch.materials().size(); i++) {
            String name = batch.materials().get(i).materialName();
            BigDecimal weighed = parseWeight(weighedFields.get(i).getText());
            if (weighed == null) {
                banner.showError("Enter what " + name + " weighed in kg: a number of at least 0, "
                        + "with at most 2 decimal places.");
                weighedFields.get(i).requestFocus();
                return;
            }
            materials.add(new BatchCycleRequestDto.Material(name, weighed));
        }

        int cycleNumber = nextCycle;
        setRunningControlsDisabled(true);
        apiClient.recordCycle(new BatchCycleRequestDto(batch.batchNumber(), cycleNumber, Instant.now(), materials))
                .thenCompose(recorded -> apiClient.batch(batch.id()))
                .whenComplete((updated, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        banner.showError(message(throwable));
                        // Whatever happened, show the batch as it now stands.
                        loadRunningBatch();
                    } else if (updated.status() == BatchStatus.COMPLETED) {
                        onCompleted(updated, cycleNumber);
                    } else {
                        banner.showSuccess("Cycle " + cycleNumber + " recorded for batch " + batch.batchNumber() + ".");
                        loadRunningBatch();
                    }
                }));
    }

    /**
     * The last planned cycle finished the batch. Said in a dialog rather than the
     * banner, which clears itself after a few seconds - this is the moment the
     * operator needs to know where the order stands.
     */
    private void onCompleted(BatchDto batch, int cycles) {
        showSetup();
        orderApiClient.list().whenComplete((orders, throwable) -> Platform.runLater(() -> {
            String order = throwable != null ? "" : orders.stream()
                    .filter(o -> o.id().equals(batch.orderId()))
                    .findFirst()
                    .map(o -> "\n\nOrder #" + o.id() + " is now " + o.status() + ": "
                            + trim(o.producedQuantityKg()) + " of " + trim(o.quantityKg()) + " kg produced.")
                    .orElse("");
            Alert done = new Alert(Alert.AlertType.INFORMATION);
            done.setTitle("Batch completed");
            done.setHeaderText("Batch " + batch.batchNumber() + " is completed");
            done.setContentText(cycles + " cycles, " + trim(batch.producedQuantity()) + " kg produced." + order
                    + "\n\nIts report, with every cycle, is under Batch Reports.");
            done.showAndWait();
            loadReferenceLists();
        }));
    }

    private void stopBatch() {
        BatchDto batch = runningBatch;
        int recorded = nextCycle - 1;
        String ofPlanned = batch.cycleNumber() == null ? "" : " of " + batch.cycleNumber();
        if (!ConfirmDialogs.confirm("Stop batch", "Stop batch " + batch.batchNumber() + "?",
                recorded + ofPlanned + " cycles have been recorded. A stopped batch takes no more cycles "
                        + "and cannot be completed afterwards.")) {
            return;
        }
        setRunningControlsDisabled(true);
        apiClient.stop(batch.id()).whenComplete((stopped, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                banner.showError(message(throwable));
                setRunningControlsDisabled(false);
                return;
            }
            banner.showSuccess("Batch " + stopped.batchNumber() + " stopped after " + recorded + ofPlanned + " cycles.");
            showSetup();
            loadReferenceLists();
        }));
    }

    private void setRunningControlsDisabled(boolean disabled) {
        recordCycleButton.setDisable(disabled);
        stopBatchButton.setDisable(disabled);
    }

    /** A weight as the API takes it: not negative, at most two decimal places. */
    private BigDecimal parseWeight(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            boolean fits = value.signum() >= 0 && value.stripTrailingZeros().scale() <= 2
                    && value.compareTo(new BigDecimal("999999.99")) <= 0;
            return fits ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Label columnHeading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
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
