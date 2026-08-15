package com.smartbatch360.desktop.driver;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Add/Edit dialog for Driver. Fields match exactly what the Driver Management mockup shows. */
public class DriverFormDialog {

    private final DriverApiClient apiClient = new DriverApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField licenseNoField = new TextField();
    private final ComboBox<DriverStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(DriverStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private DriverDto saved;

    public DriverFormDialog(DriverDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Driver" : "Add Driver");
        formDialog.addField("Driver Name", "name", nameField);
        formDialog.addField("Phone", "phone", phoneField);
        formDialog.addField("License No.", "licenseNo", licenseNoField);
        formDialog.addField("Status", "status", statusField);

        statusField.getSelectionModel().select(DriverStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            phoneField.setText(existing.phone());
            licenseNoField.setText(existing.licenseNo());
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void save() {
        formDialog.clearErrors();
        DriverRequestDto request = new DriverRequestDto(
                nameField.getText(), phoneField.getText(), licenseNoField.getText(), statusField.getValue());

        formDialog.setSaving(true);
        CompletableFuture<DriverDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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

    public Optional<DriverDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
