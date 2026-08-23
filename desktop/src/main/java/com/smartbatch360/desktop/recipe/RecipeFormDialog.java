package com.smartbatch360.desktop.recipe;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Recipe. Unlike every other master-data form, this one
 * has a genuine parent+child-list shape: the Material Proportion table is
 * user-editable inline (add/remove rows), not a fixed set of fields.
 */
public class RecipeFormDialog {

    private final RecipeApiClient apiClient = new RecipeApiClient();
    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final TextField batchSizeField = new TextField();
    private final TextField descriptionField = new TextField();
    private final ComboBox<RecipeStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(RecipeStatus.values()));

    private final ObservableList<RecipeMaterialRow> materialRows = FXCollections.observableArrayList();
    private final TableView<RecipeMaterialRow> materialsTable = new TableView<>(materialRows);

    private final boolean isEdit;
    private final Long id;
    private RecipeDto saved;

    public RecipeFormDialog(RecipeDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Recipe" : "Add Recipe");
        formDialog.addField("Recipe Name", "name", nameField);
        formDialog.addField("Batch Size (m³)", "batchSize", batchSizeField);
        formDialog.addField("Description", "description", descriptionField);
        formDialog.addField("Status", "status", statusField);
        formDialog.addField("Material Proportions", "materials", buildMaterialsEditor());

        statusField.getSelectionModel().select(RecipeStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            batchSizeField.setText(existing.batchSize() != null ? existing.batchSize().toPlainString() : "");
            descriptionField.setText(existing.description());
            statusField.getSelectionModel().select(existing.status());
            existing.materials().forEach(m -> materialRows.add(new RecipeMaterialRow(
                    m.materialName(), m.quantity() != null ? m.quantity().toPlainString() : "", m.unit())));
        }
        if (materialRows.isEmpty()) {
            materialRows.add(new RecipeMaterialRow());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private VBox buildMaterialsEditor() {
        materialsTable.setEditable(true);
        materialsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        materialsTable.setPrefHeight(160);

        TableColumn<RecipeMaterialRow, String> nameCol = new TableColumn<>("Material");
        nameCol.setCellValueFactory(cd -> cd.getValue().materialNameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getRowValue().materialNameProperty().set(e.getNewValue()));

        TableColumn<RecipeMaterialRow, String> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(cd -> cd.getValue().quantityProperty());
        quantityCol.setCellFactory(TextFieldTableCell.forTableColumn());
        quantityCol.setOnEditCommit(e -> e.getRowValue().quantityProperty().set(e.getNewValue()));

        TableColumn<RecipeMaterialRow, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(cd -> cd.getValue().unitProperty());
        unitCol.setCellFactory(TextFieldTableCell.forTableColumn());
        unitCol.setOnEditCommit(e -> e.getRowValue().unitProperty().set(e.getNewValue()));

        TableColumn<RecipeMaterialRow, Void> removeCol = new TableColumn<>("");
        removeCol.setMinWidth(70);
        removeCol.setMaxWidth(70);
        removeCol.setSortable(false);
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeButton = new Button("Remove");

            {
                removeButton.getStyleClass().add("button-danger");
                removeButton.setOnAction(e -> materialRows.remove(getIndex()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });

        materialsTable.getColumns().setAll(List.of(nameCol, quantityCol, unitCol, removeCol));

        Button addRowButton = new Button("+ Add Material");
        addRowButton.getStyleClass().add("button-secondary");
        addRowButton.setOnAction(e -> materialRows.add(new RecipeMaterialRow()));

        return new VBox(6, materialsTable, new HBox(addRowButton));
    }

    private void save() {
        formDialog.clearErrors();

        BigDecimal batchSize = parseDecimal(batchSizeField.getText());
        if (batchSize == null) {
            formDialog.setFormError("Batch size must be a valid number greater than zero.");
            return;
        }

        List<RecipeMaterialRequestDto> materials = new ArrayList<>();
        for (RecipeMaterialRow row : materialRows) {
            boolean allBlank = isBlank(row.getMaterialName()) && isBlank(row.getQuantity()) && isBlank(row.getUnit());
            if (allBlank) {
                continue;
            }
            BigDecimal quantity = parseDecimal(row.getQuantity());
            if (isBlank(row.getMaterialName()) || quantity == null || isBlank(row.getUnit())) {
                formDialog.setFormError("Every material row needs a name, a valid quantity, and a unit "
                        + "(or remove the row).");
                return;
            }
            materials.add(new RecipeMaterialRequestDto(row.getMaterialName().trim(), quantity, row.getUnit().trim()));
        }
        if (materials.isEmpty()) {
            formDialog.setFormError("At least one material is required.");
            return;
        }

        RecipeRequestDto request = new RecipeRequestDto(
                nameField.getText(), batchSize, descriptionField.getText(), statusField.getValue(), materials);

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

    private BigDecimal parseDecimal(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
