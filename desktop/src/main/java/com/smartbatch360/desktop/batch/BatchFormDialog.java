package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.client.ClientApiClient;
import com.smartbatch360.desktop.client.ClientDto;
import com.smartbatch360.desktop.common.FormDialog;
import com.smartbatch360.desktop.driver.DriverApiClient;
import com.smartbatch360.desktop.driver.DriverDto;
import com.smartbatch360.desktop.recipe.RecipeApiClient;
import com.smartbatch360.desktop.recipe.RecipeDto;
import com.smartbatch360.desktop.site.SiteApiClient;
import com.smartbatch360.desktop.site.SiteDto;
import com.smartbatch360.desktop.vehicle.VehicleApiClient;
import com.smartbatch360.desktop.vehicle.VehicleDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Batch. Like Recipe, the Material Consumption table is
 * an inline editable list rather than fixed fields - here with
 * target/setpoint/achieved per row. Selecting a Recipe auto-fills the
 * materials table from its proportions (target=setpoint=recipe quantity,
 * achieved=0) when creating a new batch with no materials entered yet.
 */
public class BatchFormDialog {

    private final BatchApiClient apiClient = new BatchApiClient();
    private final RecipeApiClient recipeApiClient = new RecipeApiClient();
    private final ClientApiClient clientApiClient = new ClientApiClient();
    private final SiteApiClient siteApiClient = new SiteApiClient();
    private final VehicleApiClient vehicleApiClient = new VehicleApiClient();
    private final DriverApiClient driverApiClient = new DriverApiClient();

    private final FormDialog formDialog;
    private final TextField batchNumberField = new TextField();
    private final ComboBox<RecipeDto> recipeField = new ComboBox<>();
    private final ComboBox<ClientDto> clientField = new ComboBox<>();
    private final ComboBox<SiteDto> siteField = new ComboBox<>();
    private final ComboBox<VehicleDto> vehicleField = new ComboBox<>();
    private final ComboBox<DriverDto> driverField = new ComboBox<>();
    private final TextField targetQuantityField = new TextField();
    private final TextField producedQuantityField = new TextField();
    private final TextField cycleNumberField = new TextField();
    private final TextField shiftField = new TextField();
    private final ComboBox<BatchStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(BatchStatus.values()));
    private final ComboBox<EquipmentStatus> mixerField = equipmentCombo();
    private final ComboBox<EquipmentStatus> conveyorField = equipmentCombo();
    private final ComboBox<EquipmentStatus> waterValveField = equipmentCombo();
    private final ComboBox<EquipmentStatus> cementScrewField = equipmentCombo();
    private final ComboBox<EquipmentStatus> compressorField = equipmentCombo();

    private final ObservableList<BatchMaterialRow> materialRows = FXCollections.observableArrayList();
    private final TableView<BatchMaterialRow> materialsTable = new TableView<>(materialRows);

    private final boolean isEdit;
    private final Long id;
    private final Long existingRecipeId;
    private final Long existingClientId;
    private final Long existingSiteId;
    private final Long existingVehicleId;
    private final Long existingDriverId;
    private BatchDto saved;

    public BatchFormDialog(BatchDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;
        this.existingRecipeId = existing != null ? existing.recipeId() : null;
        this.existingClientId = existing != null ? existing.clientId() : null;
        this.existingSiteId = existing != null ? existing.siteId() : null;
        this.existingVehicleId = existing != null ? existing.vehicleId() : null;
        this.existingDriverId = existing != null ? existing.driverId() : null;

        formDialog = new FormDialog(isEdit ? "Edit Batch" : "Add Batch");
        formDialog.addField("Batch Number", "batchNumber", batchNumberField);
        formDialog.addField("Recipe", "recipeId", recipeField);
        formDialog.addField("Client", "clientId", clientField);
        formDialog.addField("Site", "siteId", siteField);
        formDialog.addField("Vehicle", "vehicleId", vehicleField);
        formDialog.addField("Driver", "driverId", driverField);
        formDialog.addField("Target Quantity (m³)", "targetQuantity", targetQuantityField);
        formDialog.addField("Produced Quantity (m³)", "producedQuantity", producedQuantityField);
        formDialog.addField("Cycle Number", "cycleNumber", cycleNumberField);
        formDialog.addField("Shift", "shift", shiftField);
        formDialog.addField("Status", "status", statusField);
        formDialog.addField("Mixer", "mixerStatus", mixerField);
        formDialog.addField("Conveyor", "conveyorStatus", conveyorField);
        formDialog.addField("Water Valve", "waterValveStatus", waterValveField);
        formDialog.addField("Cement Screw", "cementScrewStatus", cementScrewField);
        formDialog.addField("Compressor", "compressorStatus", compressorField);
        formDialog.addField("Material Consumption", "materials", buildMaterialsEditor());

        statusField.getSelectionModel().select(BatchStatus.PENDING);
        mixerField.getSelectionModel().select(EquipmentStatus.STOPPED);
        conveyorField.getSelectionModel().select(EquipmentStatus.STOPPED);
        waterValveField.getSelectionModel().select(EquipmentStatus.STOPPED);
        cementScrewField.getSelectionModel().select(EquipmentStatus.STOPPED);
        compressorField.getSelectionModel().select(EquipmentStatus.STOPPED);
        producedQuantityField.setText("0");

        if (existing != null) {
            batchNumberField.setText(existing.batchNumber());
            targetQuantityField.setText(existing.targetQuantity() != null ? existing.targetQuantity().toPlainString() : "");
            producedQuantityField.setText(existing.producedQuantity() != null ? existing.producedQuantity().toPlainString() : "0");
            cycleNumberField.setText(existing.cycleNumber() != null ? String.valueOf(existing.cycleNumber()) : "");
            shiftField.setText(existing.shift());
            statusField.getSelectionModel().select(existing.status());
            mixerField.getSelectionModel().select(existing.mixerStatus());
            conveyorField.getSelectionModel().select(existing.conveyorStatus());
            waterValveField.getSelectionModel().select(existing.waterValveStatus());
            cementScrewField.getSelectionModel().select(existing.cementScrewStatus());
            compressorField.getSelectionModel().select(existing.compressorStatus());
            existing.materials().forEach(m -> materialRows.add(new BatchMaterialRow(
                    m.materialName(),
                    m.target() != null ? m.target().toPlainString() : "",
                    m.setpoint() != null ? m.setpoint().toPlainString() : "",
                    m.achieved() != null ? m.achieved().toPlainString() : "",
                    m.unit())));
        }

        loadReferenceLists();

        recipeField.valueProperty().addListener((obs, old, selected) -> {
            if (selected != null && materialRows.isEmpty()) {
                selected.materials().forEach(m -> materialRows.add(new BatchMaterialRow(
                        m.materialName(), m.quantity().toPlainString(), m.quantity().toPlainString(), "0", m.unit())));
            }
        });

        formDialog.interceptSaveClose(event -> save());
    }

    private static ComboBox<EquipmentStatus> equipmentCombo() {
        return new ComboBox<>(FXCollections.observableArrayList(EquipmentStatus.values()));
    }

    private void loadReferenceLists() {
        recipeField.setDisable(true);
        recipeApiClient.list().whenComplete((items, throwable) -> Platform.runLater(() ->
                populateCombo(recipeField, throwable == null ? items : List.of(), RecipeDto::id, existingRecipeId,
                        "No recipes exist yet. Add a recipe before creating a batch.")));

        clientField.setDisable(true);
        clientApiClient.list().whenComplete((items, throwable) -> Platform.runLater(() ->
                populateCombo(clientField, throwable == null ? items : List.of(), ClientDto::id, existingClientId,
                        "No clients exist yet. Add a client before creating a batch.")));

        siteField.setDisable(true);
        siteApiClient.list().whenComplete((items, throwable) -> Platform.runLater(() ->
                populateCombo(siteField, throwable == null ? items : List.of(), SiteDto::id, existingSiteId,
                        "No sites exist yet. Add a site before creating a batch.")));

        vehicleField.setDisable(true);
        vehicleApiClient.list().whenComplete((items, throwable) -> Platform.runLater(() ->
                populateCombo(vehicleField, throwable == null ? items : List.of(), VehicleDto::id, existingVehicleId,
                        "No vehicles exist yet. Add a vehicle before creating a batch.")));

        driverField.setDisable(true);
        driverApiClient.list().whenComplete((items, throwable) -> Platform.runLater(() ->
                populateCombo(driverField, throwable == null ? items : List.of(), DriverDto::id, existingDriverId,
                        "No drivers exist yet. Add a driver before creating a batch.")));
    }

    private <T> void populateCombo(ComboBox<T> combo, List<T> items, java.util.function.Function<T, Long> idOf,
                                    Long existingId, String emptyMessage) {
        combo.setItems(FXCollections.observableArrayList(items));
        combo.setDisable(false);
        items.stream()
                .filter(item -> existingId != null && existingId.equals(idOf.apply(item)))
                .findFirst()
                .ifPresent(combo.getSelectionModel()::select);
        if (items.isEmpty()) {
            formDialog.setFormError(emptyMessage);
        }
    }

    private VBox buildMaterialsEditor() {
        materialsTable.setEditable(true);
        materialsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        materialsTable.setPrefHeight(160);

        TableColumn<BatchMaterialRow, String> nameCol = new TableColumn<>("Material");
        nameCol.setCellValueFactory(cd -> cd.getValue().materialNameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getRowValue().materialNameProperty().set(e.getNewValue()));

        TableColumn<BatchMaterialRow, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(cd -> cd.getValue().targetProperty());
        targetCol.setCellFactory(TextFieldTableCell.forTableColumn());
        targetCol.setOnEditCommit(e -> e.getRowValue().targetProperty().set(e.getNewValue()));

        TableColumn<BatchMaterialRow, String> setpointCol = new TableColumn<>("Setpoint");
        setpointCol.setCellValueFactory(cd -> cd.getValue().setpointProperty());
        setpointCol.setCellFactory(TextFieldTableCell.forTableColumn());
        setpointCol.setOnEditCommit(e -> e.getRowValue().setpointProperty().set(e.getNewValue()));

        TableColumn<BatchMaterialRow, String> achievedCol = new TableColumn<>("Achieved");
        achievedCol.setCellValueFactory(cd -> cd.getValue().achievedProperty());
        achievedCol.setCellFactory(TextFieldTableCell.forTableColumn());
        achievedCol.setOnEditCommit(e -> e.getRowValue().achievedProperty().set(e.getNewValue()));

        TableColumn<BatchMaterialRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(cd -> cd.getValue().unitProperty());
        unitCol.setCellFactory(TextFieldTableCell.forTableColumn());
        unitCol.setOnEditCommit(e -> e.getRowValue().unitProperty().set(e.getNewValue()));

        TableColumn<BatchMaterialRow, Void> removeCol = new TableColumn<>("");
        removeCol.setMinWidth(70);
        removeCol.setMaxWidth(70);
        removeCol.setSortable(false);
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeButton = new Button("Remove");

            {
                removeButton.getStyleClass().add("button-danger");
                removeButton.setOnAction(e -> materialRows.remove(getIndex()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });

        materialsTable.getColumns().setAll(List.of(nameCol, targetCol, setpointCol, achievedCol, unitCol, removeCol));

        Button addRowButton = new Button("+ Add Material");
        addRowButton.getStyleClass().add("button-secondary");
        addRowButton.setOnAction(e -> materialRows.add(new BatchMaterialRow()));

        Label hint = new Label("Selecting a Recipe auto-fills this table when it's empty.");
        hint.getStyleClass().add("state-message");

        return new VBox(6, materialsTable, new HBox(addRowButton), hint);
    }

    private void save() {
        formDialog.clearErrors();

        RecipeDto recipe = recipeField.getValue();
        ClientDto client = clientField.getValue();
        SiteDto site = siteField.getValue();
        VehicleDto vehicle = vehicleField.getValue();
        DriverDto driver = driverField.getValue();

        if (recipe == null || client == null || site == null || vehicle == null || driver == null) {
            formDialog.setFormError("Recipe, Client, Site, Vehicle and Driver are all required.");
            return;
        }

        BigDecimal targetQuantity = parseDecimal(targetQuantityField.getText());
        if (targetQuantity == null) {
            formDialog.setFormError("Target quantity must be a valid number greater than zero.");
            return;
        }
        BigDecimal producedQuantity = parseNonNegativeDecimal(producedQuantityField.getText());
        if (producedQuantity == null) {
            formDialog.setFormError("Produced quantity must be a valid number (zero or more).");
            return;
        }
        Integer cycleNumber = parseIntOrNull(cycleNumberField.getText());

        List<BatchMaterialRequestDto> materials = new ArrayList<>();
        for (BatchMaterialRow row : materialRows) {
            boolean allBlank = isBlank(row.getMaterialName()) && isBlank(row.getTarget())
                    && isBlank(row.getSetpoint()) && isBlank(row.getAchieved()) && isBlank(row.getUnit());
            if (allBlank) {
                continue;
            }
            BigDecimal target = parseNonNegativeDecimal(row.getTarget());
            BigDecimal setpoint = parseNonNegativeDecimal(row.getSetpoint());
            BigDecimal achieved = parseNonNegativeDecimal(row.getAchieved());
            if (isBlank(row.getMaterialName()) || target == null || setpoint == null || achieved == null || isBlank(row.getUnit())) {
                formDialog.setFormError("Every material row needs a name, valid target/setpoint/achieved values, "
                        + "and a unit (or remove the row).");
                return;
            }
            materials.add(new BatchMaterialRequestDto(row.getMaterialName().trim(), target, setpoint, achieved, row.getUnit().trim()));
        }
        if (materials.isEmpty()) {
            formDialog.setFormError("At least one material is required.");
            return;
        }

        BatchRequestDto request = new BatchRequestDto(
                batchNumberField.getText(), recipe.id(), client.id(), site.id(), vehicle.id(), driver.id(),
                targetQuantity, producedQuantity, null, cycleNumber, shiftField.getText(),
                statusField.getValue(), mixerField.getValue(), conveyorField.getValue(),
                waterValveField.getValue(), cementScrewField.getValue(), compressorField.getValue(), materials);

        formDialog.setSaving(true);
        CompletableFuture<BatchDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

        future.whenComplete((result, throwable) -> Platform.runLater(() -> {
            formDialog.setSaving(false);
            if (throwable != null) {
                handleError(throwable);
            } else {
                saved = result;
                formDialog.close();
            }
        }));
    }

    private BigDecimal parseDecimal(String text) {
        BigDecimal value = parseNonNegativeDecimal(text);
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
    }

    private BigDecimal parseNonNegativeDecimal(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            return value.compareTo(BigDecimal.ZERO) >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void handleError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof ApiException apiEx && !apiEx.fieldErrors().isEmpty()) {
            formDialog.applyFieldErrors(apiEx.fieldErrors());
        } else if (cause instanceof ApiException apiEx) {
            formDialog.setFormError(apiEx.getMessage());
        } else {
            formDialog.setFormError("Something went wrong. Please try again.");
        }
    }

    public Optional<BatchDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
