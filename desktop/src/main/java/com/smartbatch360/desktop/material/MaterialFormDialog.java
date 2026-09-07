package com.smartbatch360.desktop.material;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Material: just a name. The unit and density that used to
 * be here went with the m³ conversion they existed for - every material is
 * weighed in kilograms as of 2026-09-07.
 */
public class MaterialFormDialog {

    private final MaterialApiClient apiClient = new MaterialApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();

    private final boolean isEdit;
    private final Long id;
    private MaterialDto saved;

    public MaterialFormDialog(MaterialDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Material" : "Add Material");
        formDialog.addField("Material Name", "name", nameField);

        if (existing != null) {
            nameField.setText(existing.name());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void save() {
        formDialog.clearErrors();

        MaterialRequestDto request = new MaterialRequestDto(nameField.getText());

        formDialog.setSaving(true);
        CompletableFuture<MaterialDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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

    public Optional<MaterialDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
