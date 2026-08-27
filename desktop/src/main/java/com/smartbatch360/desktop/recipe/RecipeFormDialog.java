package com.smartbatch360.desktop.recipe;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import com.smartbatch360.desktop.material.MaterialApiClient;
import com.smartbatch360.desktop.material.MaterialDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Recipe. Reworked 2026-08-27: the user names the recipe,
 * then uses "Add Material" to pick real Material records and give each a
 * quantity, instead of typing material names and units as free text.
 *
 * The total batch quantity shown here is a live PREVIEW only. The stored value
 * is always the one the backend derives on save (Recipe#recalculateTotalBatchQuantity) -
 * this mirrors that formula so the number updates as rows change, and both use
 * the same inputs (material density / 1000 L per m3).
 */
public class RecipeFormDialog {

    private static final BigDecimal LITRES_PER_CUBIC_METRE = new BigDecimal("1000");
    private static final int CONVERSION_SCALE = 10;
    private static final int TOTAL_SCALE = 4;

    private final RecipeApiClient apiClient = new RecipeApiClient();
    private final MaterialApiClient materialApiClient = new MaterialApiClient();

    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final TextField descriptionField = new TextField();
    private final ComboBox<RecipeStatus> statusField =
            new ComboBox<>(FXCollections.observableArrayList(RecipeStatus.values()));

    private final ObservableList<MaterialDto> availableMaterials = FXCollections.observableArrayList();
    private final ObservableList<RecipeMaterialRow> materialRows = FXCollections.observableArrayList();
    private final TableView<RecipeMaterialRow> materialsTable = new TableView<>(materialRows);
    private final Label totalLabel = new Label();

    private final boolean isEdit;
    private final Long id;
    private final List<RecipeMaterialDto> existingMaterials;
    private RecipeDto saved;

    public RecipeFormDialog(RecipeDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;
        this.existingMaterials = existing != null ? existing.materials() : List.of();

        formDialog = new FormDialog(isEdit ? "Edit Recipe" : "Add Recipe");
        formDialog.addField("Recipe Name", "name", nameField);
        formDialog.addField("Description", "description", descriptionField);
        formDialog.addField("Status", "status", statusField);
        formDialog.addField("Materials", "materials", buildMaterialsEditor());
        formDialog.addField("Total Batch Quantity", "totalBatchQuantityM3", totalLabel);

        statusField.getSelectionModel().select(RecipeStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            descriptionField.setText(existing.description());
            statusField.getSelectionModel().select(existing.status());
        }

        loadMaterials();
        refreshTotal();

        formDialog.interceptSaveClose(event -> save());
    }

    private VBox buildMaterialsEditor() {
        materialsTable.setEditable(true);
        materialsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        materialsTable.setPrefHeight(170);
        materialsTable.setPlaceholder(new Label("No materials yet - use \"+ Add Material\"."));

        TableColumn<RecipeMaterialRow, MaterialDto> materialCol = new TableColumn<>("Material");
        materialCol.setCellValueFactory(cd -> cd.getValue().materialProperty());
        materialCol.setCellFactory(ComboBoxTableCell.forTableColumn(availableMaterials));
        materialCol.setOnEditCommit(e -> {
            e.getRowValue().materialProperty().set(e.getNewValue());
            materialsTable.refresh();
            refreshTotal();
        });

        TableColumn<RecipeMaterialRow, String> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(cd -> cd.getValue().quantityProperty());
        quantityCol.setCellFactory(TextFieldTableCell.forTableColumn());
        quantityCol.setOnEditCommit(e -> {
            e.getRowValue().quantityProperty().set(e.getNewValue());
            refreshTotal();
        });

        // Read-only: the unit always comes from the chosen material.
        TableColumn<RecipeMaterialRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setSortable(false);
        unitCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getUnitLabel()));

        TableColumn<RecipeMaterialRow, Void> removeCol = new TableColumn<>("");
        removeCol.setMinWidth(70);
        removeCol.setMaxWidth(70);
        removeCol.setSortable(false);
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeButton = new Button("Remove");

            {
                removeButton.getStyleClass().add("button-danger");
                removeButton.setOnAction(e -> {
                    materialRows.remove(getIndex());
                    refreshTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });

        materialsTable.getColumns().setAll(List.of(materialCol, quantityCol, unitCol, removeCol));

        Button addRowButton = new Button("+ Add Material");
        addRowButton.getStyleClass().add("button-secondary");
        addRowButton.setOnAction(e -> {
            materialRows.add(new RecipeMaterialRow());
            refreshTotal();
        });

        Label hint = new Label("Pick a material, then enter the quantity in that material's unit.");
        hint.getStyleClass().add("state-message");

        return new VBox(6, materialsTable, new HBox(addRowButton), hint);
    }

    private void loadMaterials() {
        materialApiClient.list().whenComplete((materials, throwable) -> Platform.runLater(() -> {
            List<MaterialDto> items = throwable == null ? materials : List.of();
            availableMaterials.setAll(items);

            // Re-link an edited recipe's existing rows to the loaded material
            // instances, so the ComboBox shows them as selected.
            if (materialRows.isEmpty() && !existingMaterials.isEmpty()) {
                for (RecipeMaterialDto line : existingMaterials) {
                    items.stream()
                            .filter(m -> m.id().equals(line.materialId()))
                            .findFirst()
                            .ifPresent(m -> materialRows.add(new RecipeMaterialRow(
                                    m, line.quantity() != null ? line.quantity().toPlainString() : "")));
                }
            }
            if (materialRows.isEmpty()) {
                materialRows.add(new RecipeMaterialRow());
            }
            if (items.isEmpty()) {
                formDialog.setFormError("No materials exist yet. Add materials before creating a recipe.");
            }
            materialsTable.refresh();
            refreshTotal();
        }));
    }

    /**
     * Live preview of what the backend will compute on save. Rows that aren't
     * usable yet (no material picked, blank/invalid quantity, or a weight
     * material with no density) are simply skipped rather than guessed at.
     */
    private void refreshTotal() {
        BigDecimal total = BigDecimal.ZERO;
        boolean incomplete = false;
        boolean missingDensity = false;

        for (RecipeMaterialRow row : materialRows) {
            MaterialDto material = row.getMaterial();
            BigDecimal quantity = row.parsedQuantity();
            if (material == null || quantity == null) {
                incomplete = true;
                continue;
            }
            BigDecimal divisor;
            if (material.unit().requiresDensity()) {
                if (material.densityKgPerM3() == null
                        || material.densityKgPerM3().compareTo(BigDecimal.ZERO) <= 0) {
                    missingDensity = true;
                    continue;
                }
                divisor = material.densityKgPerM3();
            } else {
                divisor = LITRES_PER_CUBIC_METRE;
            }
            total = total.add(quantity.divide(divisor, CONVERSION_SCALE, RoundingMode.HALF_UP));
        }

        StringBuilder text = new StringBuilder(total.setScale(TOTAL_SCALE, RoundingMode.HALF_UP).toPlainString());
        text.append(" m³");
        if (missingDensity) {
            text.append("  (some materials have no density set - fix under Materials)");
        } else if (incomplete) {
            text.append("  (incomplete rows not counted)");
        }
        totalLabel.setText(text.toString());
    }

    private void save() {
        formDialog.clearErrors();

        List<RecipeMaterialRequestDto> materials = new ArrayList<>();
        for (RecipeMaterialRow row : materialRows) {
            if (row.getMaterial() == null && (row.getQuantity() == null || row.getQuantity().isBlank())) {
                continue; // untouched blank row
            }
            BigDecimal quantity = row.parsedQuantity();
            if (row.getMaterial() == null || quantity == null) {
                formDialog.setFormError("Every material row needs a material and a quantity greater than zero "
                        + "(or remove the row).");
                return;
            }
            materials.add(new RecipeMaterialRequestDto(row.getMaterial().id(), quantity));
        }
        if (materials.isEmpty()) {
            formDialog.setFormError("At least one material is required.");
            return;
        }

        RecipeRequestDto request = new RecipeRequestDto(
                nameField.getText(), descriptionField.getText(), statusField.getValue(), materials);

        formDialog.setSaving(true);
        CompletableFuture<RecipeDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

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

    public Optional<RecipeDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
