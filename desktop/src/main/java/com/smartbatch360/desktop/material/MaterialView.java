package com.smartbatch360.desktop.material;

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

/** Material Management list screen: the materials recipes are built from. */
public class MaterialView {

    private final MaterialApiClient apiClient = new MaterialApiClient();
    private final CrudListView<MaterialDto> listView = new CrudListView<>(
            "Material Management", "Manage the materials recipes are built from.", "+ Add Material",
            m -> String.join(" ", m.name(), m.unit().name()));

    public MaterialView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<MaterialDto> table = listView.getTable();

        TableColumn<MaterialDto, String> nameCol = new TableColumn<>("Material Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));

        TableColumn<MaterialDto, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().unit().name()));

        // Blank for LITRE - a volume unit needs no density to convert to m³.
        TableColumn<MaterialDto, String> densityCol = new TableColumn<>("Density (kg/m³)");
        densityCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().densityKgPerM3() != null
                        ? cd.getValue().densityKgPerM3().toPlainString()
                        : "-"));

        TableColumn<MaterialDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(nameCol, unitCol, densityCol, actionsCol));
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((materials, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(materials);
            }
        }));
    }

    private void openAddDialog() {
        new MaterialFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Material \"" + saved.name() + "\" created.");
            load();
        });
    }

    private void openEditDialog(MaterialDto material) {
        new MaterialFormDialog(material).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Material \"" + saved.name() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(MaterialDto material) {
        if (!ConfirmDialogs.confirmDelete("material", material.name())) {
            return;
        }
        apiClient.delete(material.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Material \"" + material.name() + "\" deleted.");
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
