package com.smartbatch360.desktop.customer;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Add/Edit dialog for Customer. Fields match exactly what the Customer Management mockup shows. */
public class CustomerFormDialog {

    private final CustomerApiClient apiClient = new CustomerApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final TextField contactPersonField = new TextField();
    private final TextField phoneField = new TextField();
    private final ComboBox<CustomerStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(CustomerStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private CustomerDto saved;

    public CustomerFormDialog(CustomerDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Customer" : "Add Customer");
        formDialog.addField("Customer Name", "name", nameField);
        formDialog.addField("Contact Person", "contactPerson", contactPersonField);
        formDialog.addField("Phone", "phone", phoneField);
        formDialog.addField("Status", "status", statusField);

        statusField.getSelectionModel().select(CustomerStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            contactPersonField.setText(existing.contactPerson());
            phoneField.setText(existing.phone());
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void save() {
        formDialog.clearErrors();
        CustomerRequestDto request = new CustomerRequestDto(
                nameField.getText(), contactPersonField.getText(), phoneField.getText(), statusField.getValue());

        formDialog.setSaving(true);
        CompletableFuture<CustomerDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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
    public Optional<CustomerDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
