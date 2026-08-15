package com.smartbatch360.desktop.driver;

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

/** Driver Management list screen: table + Add/Edit/Delete wired to the REST API. */
public class DriverView {

    private final DriverApiClient apiClient = new DriverApiClient();
    private final CrudListView<DriverDto> listView =
            new CrudListView<>("Driver Management", "Manage drivers and their performance.", "+ Add Driver");

    public DriverView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<DriverDto> table = listView.getTable();

        TableColumn<DriverDto, String> nameCol = new TableColumn<>("Driver Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));

        TableColumn<DriverDto, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().phone()));

        TableColumn<DriverDto, String> licenseCol = new TableColumn<>("License No.");
        licenseCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().licenseNo()));

        TableColumn<DriverDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<DriverDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(nameCol, phoneCol, licenseCol, statusCol, actionsCol));
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((drivers, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(drivers);
            }
        }));
    }

    private void openAddDialog() {
        new DriverFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Driver \"" + saved.name() + "\" created.");
            load();
        });
    }

    private void openEditDialog(DriverDto driver) {
        new DriverFormDialog(driver).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Driver \"" + saved.name() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(DriverDto driver) {
        if (!ConfirmDialogs.confirmDelete("driver", driver.name())) {
            return;
        }
        apiClient.delete(driver.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Driver \"" + driver.name() + "\" deleted.");
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
