package com.smartbatch360.desktop.header;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Header. Fields as clarified directly by the user (no
 * source-document mockup exists for this module): Company Name, Plant/Branch
 * Name, Address, Phone, Email, GSTIN/Tax ID, Status.
 */
public class HeaderFormDialog {

    private final HeaderApiClient apiClient = new HeaderApiClient();
    private final FormDialog formDialog;
    private final TextField companyNameField = new TextField();
    private final TextField plantNameField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField gstinField = new TextField();
    private final ComboBox<HeaderStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(HeaderStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private HeaderDto saved;

    public HeaderFormDialog(HeaderDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Header" : "Add Header");
        formDialog.addField("Company Name", "companyName", companyNameField);
        formDialog.addField("Plant/Branch Name", "plantName", plantNameField);
        formDialog.addField("Address", "address", addressField);
        formDialog.addField("Phone", "phone", phoneField);
        formDialog.addField("Email", "email", emailField);
        formDialog.addField("GSTIN / Tax ID", "gstin", gstinField);
        formDialog.addField("Status", "status", statusField);

        statusField.getSelectionModel().select(HeaderStatus.ACTIVE);
        if (existing != null) {
            companyNameField.setText(existing.companyName());
            plantNameField.setText(existing.plantName());
            addressField.setText(existing.address());
            phoneField.setText(existing.phone());
            emailField.setText(existing.email());
            gstinField.setText(existing.gstin());
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void save() {
        formDialog.clearErrors();
        HeaderRequestDto request = new HeaderRequestDto(
                companyNameField.getText(), plantNameField.getText(), addressField.getText(),
                phoneField.getText(), emailField.getText(), gstinField.getText(), statusField.getValue());

        formDialog.setSaving(true);
        CompletableFuture<HeaderDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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

    /** Shows the modal dialog; returns the saved record, or empty if the user cancelled. */
    public Optional<HeaderDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
