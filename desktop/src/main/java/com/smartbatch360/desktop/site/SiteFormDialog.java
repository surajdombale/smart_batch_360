package com.smartbatch360.desktop.site;

import com.smartbatch360.desktop.api.ApiErrorDto;
import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.client.ClientApiClient;
import com.smartbatch360.desktop.client.ClientDto;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Add/Edit dialog for Site. Fields match exactly what the Site Management mockup shows. */
public class SiteFormDialog {

    private final SiteApiClient apiClient = new SiteApiClient();
    private final ClientApiClient clientApiClient = new ClientApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final ComboBox<ClientDto> clientField = new ComboBox<>();
    private final TextField locationField = new TextField();
    private final ComboBox<SiteStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(SiteStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private final Long existingClientId;
    private SiteDto saved;

    public SiteFormDialog(SiteDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;
        this.existingClientId = existing != null ? existing.clientId() : null;

        formDialog = new FormDialog(isEdit ? "Edit Site" : "Add Site");
        formDialog.addField("Site Name", "name", nameField);
        formDialog.addField("Client", "clientId", clientField);
        formDialog.addField("Location", "location", locationField);
        formDialog.addField("Status", "status", statusField);

        clientField.setDisable(true);
        clientField.setPromptText("Loading clients...");
        loadClients();

        statusField.getSelectionModel().select(SiteStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            locationField.setText(existing.location());
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void loadClients() {
        clientApiClient.list().whenComplete((clients, throwable) -> Platform.runLater(() -> {
            List<ClientDto> items = throwable == null ? clients : List.of();
            clientField.setItems(FXCollections.observableArrayList(items));
            clientField.setDisable(false);

            items.stream()
                    .filter(c -> existingClientId != null && existingClientId.equals(c.id()))
                    .findFirst()
                    .ifPresent(c -> clientField.getSelectionModel().select(c));

            if (items.isEmpty()) {
                formDialog.setFormError("No clients exist yet. Add a client before creating a site.");
            }
        }));
    }

    private void save() {
        formDialog.clearErrors();

        ClientDto selectedClient = clientField.getValue();
        if (selectedClient == null) {
            formDialog.applyFieldErrors(List.of(new ApiErrorDto.FieldErrorDto("clientId", "Client is required.")));
            return;
        }

        SiteRequestDto request = new SiteRequestDto(
                nameField.getText(), selectedClient.id(), locationField.getText(), statusField.getValue());

        formDialog.setSaving(true);
        CompletableFuture<SiteDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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

    public Optional<SiteDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
