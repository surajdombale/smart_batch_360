package com.smartbatch360.desktop.client;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Client (originally "Customer"). Fields match what the
 * Customer Management mockup shows, plus Address - added at the user's
 * explicit request (2026-08-23), optional since it has no mockup backing.
 */
public class ClientFormDialog {

    private final ClientApiClient apiClient = new ClientApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final TextField contactPersonField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField addressField = new TextField();
    private final ComboBox<ClientStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(ClientStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private ClientDto saved;

    public ClientFormDialog(ClientDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Client" : "Add Client");
        formDialog.addField("Client Name", "name", nameField);
        formDialog.addField("Contact Person", "contactPerson", contactPersonField);
        formDialog.addField("Phone", "phone", phoneField);
        formDialog.addField("Address", "address", addressField);
        formDialog.addField("Status", "status", statusField);

        statusField.getSelectionModel().select(ClientStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            contactPersonField.setText(existing.contactPerson());
            phoneField.setText(existing.phone());
            addressField.setText(existing.address());
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void save() {
        formDialog.clearErrors();
        ClientRequestDto request = new ClientRequestDto(
                nameField.getText(), contactPersonField.getText(), phoneField.getText(),
                addressField.getText(), statusField.getValue());

        formDialog.setSaving(true);
        CompletableFuture<ClientDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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
    public Optional<ClientDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
