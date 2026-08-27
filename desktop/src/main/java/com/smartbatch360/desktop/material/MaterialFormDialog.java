package com.smartbatch360.desktop.material;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Material: name + unit, plus a density that only applies
 * to weight-based units. The density is what lets a KG quantity be converted
 * into a recipe's m³ total - it is asked for rather than assumed.
 */
public class MaterialFormDialog {

    private final MaterialApiClient apiClient = new MaterialApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final ComboBox<MaterialUnit> unitField =
            new ComboBox<>(FXCollections.observableArrayList(MaterialUnit.values()));
    private final TextField densityField = new TextField();

    private final boolean isEdit;
    private final Long id;
    private MaterialDto saved;

    public MaterialFormDialog(MaterialDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Material" : "Add Material");
        formDialog.addField("Material Name", "name", nameField);
        formDialog.addField("Unit", "unit", unitField);
        formDialog.addField("Density (kg/m³)", "densityKgPerM3", densityField);

        densityField.setPromptText("e.g. 1440 - required for KG");
        unitField.valueProperty().addListener((obs, old, unit) -> applyUnit(unit));

        unitField.getSelectionModel().select(MaterialUnit.KG);
        if (existing != null) {
            nameField.setText(existing.name());
            unitField.getSelectionModel().select(existing.unit());
            densityField.setText(existing.densityKgPerM3() != null
                    ? existing.densityKgPerM3().toPlainString() : "");
        }
        applyUnit(unitField.getValue());

        formDialog.interceptSaveClose(event -> save());
    }

    /** Density is meaningless for LITRE (1 m³ = 1000 L exactly), so it is disabled and cleared there. */
    private void applyUnit(MaterialUnit unit) {
        boolean needsDensity = unit != null && unit.requiresDensity();
        densityField.setDisable(!needsDensity);
        if (!needsDensity) {
            densityField.clear();
        }
    }

    private void save() {
        formDialog.clearErrors();

        MaterialUnit unit = unitField.getValue();
        if (unit == null) {
            formDialog.setFormError("Unit is required.");
            return;
        }

        BigDecimal density = null;
        if (unit.requiresDensity()) {
            density = parseDecimal(densityField.getText());
            if (density == null) {
                formDialog.setFormError("Materials measured in " + unit + " need a density in kg/m³ "
                        + "(greater than zero) so recipe quantities can be converted to m³.");
                return;
            }
        }

        MaterialRequestDto request = new MaterialRequestDto(nameField.getText(), unit, density);

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

    private BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
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
