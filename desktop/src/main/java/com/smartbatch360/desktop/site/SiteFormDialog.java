package com.smartbatch360.desktop.site;

import com.smartbatch360.desktop.api.ApiErrorDto;
import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import com.smartbatch360.desktop.customer.CustomerApiClient;
import com.smartbatch360.desktop.customer.CustomerDto;
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
    private final CustomerApiClient customerApiClient = new CustomerApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final ComboBox<CustomerDto> customerField = new ComboBox<>();
    private final TextField locationField = new TextField();
    private final ComboBox<SiteStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(SiteStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private final Long existingCustomerId;
    private SiteDto saved;

    public SiteFormDialog(SiteDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;
        this.existingCustomerId = existing != null ? existing.customerId() : null;

        formDialog = new FormDialog(isEdit ? "Edit Site" : "Add Site");
        formDialog.addField("Site Name", "name", nameField);
        formDialog.addField("Customer", "customerId", customerField);
        formDialog.addField("Location", "location", locationField);
        formDialog.addField("Status", "status", statusField);

        customerField.setDisable(true);
        customerField.setPromptText("Loading customers...");
        loadCustomers();

        statusField.getSelectionModel().select(SiteStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            locationField.setText(existing.location());
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void loadCustomers() {
        customerApiClient.list().whenComplete((customers, throwable) -> Platform.runLater(() -> {
            List<CustomerDto> items = throwable == null ? customers : List.of();
            customerField.setItems(FXCollections.observableArrayList(items));
            customerField.setDisable(false);

            items.stream()
                    .filter(c -> existingCustomerId != null && existingCustomerId.equals(c.id()))
                    .findFirst()
                    .ifPresent(c -> customerField.getSelectionModel().select(c));

            if (items.isEmpty()) {
                formDialog.setFormError("No customers exist yet. Add a customer before creating a site.");
            }
        }));
    }

    private void save() {
        formDialog.clearErrors();

        CustomerDto selectedCustomer = customerField.getValue();
        if (selectedCustomer == null) {
            formDialog.applyFieldErrors(List.of(new ApiErrorDto.FieldErrorDto("customerId", "Customer is required.")));
            return;
        }

        SiteRequestDto request = new SiteRequestDto(
                nameField.getText(), selectedCustomer.id(), locationField.getText(), statusField.getValue());

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
