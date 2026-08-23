package com.smartbatch360.desktop.recipe;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.ActionsColumn;
import com.smartbatch360.desktop.common.ConfirmDialogs;
import com.smartbatch360.desktop.common.CrudListView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.stream.Collectors;

/** Recipe Management list screen: table + Add/Edit/Delete wired to the REST API. */
public class RecipeView {

    private final RecipeApiClient apiClient = new RecipeApiClient();
    private final CrudListView<RecipeDto> listView = new CrudListView<>(
            "Recipe Management", "Manage concrete mix recipes and material proportions.", "+ Add Recipe",
            r -> String.join(" ", r.name(), emptyIfNull(r.description()), r.status().name(), materialNames(r)));

    public RecipeView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<RecipeDto> table = listView.getTable();

        TableColumn<RecipeDto, String> nameCol = new TableColumn<>("Recipe Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));

        TableColumn<RecipeDto, String> batchSizeCol = new TableColumn<>("Batch Size");
        batchSizeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().batchSize() + " m³"));

        TableColumn<RecipeDto, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(cd -> new SimpleStringProperty(emptyIfNull(cd.getValue().description())));

        TableColumn<RecipeDto, String> materialsCol = new TableColumn<>("Materials");
        materialsCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().materials().size() + " items"));

        TableColumn<RecipeDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<RecipeDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(nameCol, batchSizeCol, descriptionCol, materialsCol, statusCol, actionsCol));
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private static String materialNames(RecipeDto recipe) {
        return recipe.materials().stream().map(RecipeMaterialDto::materialName).collect(Collectors.joining(" "));
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((recipes, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(recipes);
            }
        }));
    }

    private void openAddDialog() {
        new RecipeFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Recipe \"" + saved.name() + "\" created.");
            load();
        });
    }

    private void openEditDialog(RecipeDto recipe) {
        new RecipeFormDialog(recipe).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Recipe \"" + saved.name() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(RecipeDto recipe) {
        if (!ConfirmDialogs.confirmDelete("recipe", recipe.name())) {
            return;
        }
        apiClient.delete(recipe.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Recipe \"" + recipe.name() + "\" deleted.");
                load();
            }
        }));
    }

    private String errorMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause instanceof ApiException apiEx ? apiEx.getMessage() : "Something went wrong. Please try again.";
    }

    public Region getView() {
        return listView.getView();
    }
}
