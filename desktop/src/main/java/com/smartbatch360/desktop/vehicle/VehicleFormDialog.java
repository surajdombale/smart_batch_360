package com.smartbatch360.desktop.vehicle;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import com.smartbatch360.desktop.driver.DriverApiClient;
import com.smartbatch360.desktop.driver.DriverDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Add/Edit dialog for Vehicle. Fields match exactly what the Vehicle Management mockup shows. */
public class VehicleFormDialog {

    /** Sentinel entry representing "no driver assigned" in the driver dropdown. */
    private static final DriverDto NO_DRIVER =
            new DriverDto(null, "— No driver assigned —", null, null, null, null, null);

    private final VehicleApiClient apiClient = new VehicleApiClient();
    private final DriverApiClient driverApiClient = new DriverApiClient();
    private final FormDialog formDialog;
    private final TextField vehicleNumberField = new TextField();
    private final ComboBox<DriverDto> driverField = new ComboBox<>();
    private final TextField capacityField = new TextField();
    private final ComboBox<VehicleStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(VehicleStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private final Long existingDriverId;
    private VehicleDto saved;

    public VehicleFormDialog(VehicleDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;
        this.existingDriverId = existing != null ? existing.driverId() : null;

        formDialog = new FormDialog(isEdit ? "Edit Vehicle" : "Add Vehicle");
        formDialog.addField("Vehicle Number", "vehicleNumber", vehicleNumberField);
        formDialog.addField("Driver", "driverId", driverField);
        formDialog.addField("Capacity (m³)", "capacityCubicMeters", capacityField);
        formDialog.addField("Status", "status", statusField);

        driverField.setDisable(true);
        driverField.setPromptText("Loading drivers...");
        loadDrivers();

        statusField.getSelectionModel().select(VehicleStatus.AVAILABLE);
        if (existing != null) {
            vehicleNumberField.setText(existing.vehicleNumber());
            capacityField.setText(existing.capacityCubicMeters().toPlainString());
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void loadDrivers() {
        driverApiClient.list().whenComplete((drivers, throwable) -> Platform.runLater(() -> {
            List<DriverDto> items = new ArrayList<>();
            items.add(NO_DRIVER);
            if (throwable == null) {
                items.addAll(drivers);
            }
            driverField.setItems(FXCollections.observableArrayList(items));
            driverField.setDisable(false);

            DriverDto toSelect = items.stream()
                    .filter(d -> existingDriverId != null && existingDriverId.equals(d.id()))
                    .findFirst()
                    .orElse(NO_DRIVER);
            driverField.getSelectionModel().select(toSelect);
        }));
    }

    private void save() {
        formDialog.clearErrors();

        BigDecimal capacity;
        try {
            capacity = new BigDecimal(capacityField.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            formDialog.applyFieldErrors(List.of(
                    new com.smartbatch360.desktop.api.ApiErrorDto.FieldErrorDto(
                            "capacityCubicMeters", "Capacity must be a valid number.")));
            return;
        }

        DriverDto selectedDriver = driverField.getValue();
        Long driverId = (selectedDriver == null || selectedDriver.id() == null) ? null : selectedDriver.id();

        VehicleRequestDto request = new VehicleRequestDto(
                vehicleNumberField.getText(), driverId, capacity, statusField.getValue());

        formDialog.setSaving(true);
        CompletableFuture<VehicleDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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

    public Optional<VehicleDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
