package com.smartbatch360.desktop.vehicle;

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

/** Vehicle Management list screen: table + Add/Edit/Delete wired to the REST API. */
public class VehicleView {

    private final VehicleApiClient apiClient = new VehicleApiClient();
    private final CrudListView<VehicleDto> listView =
            new CrudListView<>("Vehicle Management", "Track vehicle fleet and availability.", "+ Add Vehicle");

    public VehicleView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<VehicleDto> table = listView.getTable();

        TableColumn<VehicleDto, String> numberCol = new TableColumn<>("Vehicle Number");
        numberCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().vehicleNumber()));

        TableColumn<VehicleDto, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().driverName() != null ? cd.getValue().driverName() : "—"));

        TableColumn<VehicleDto, String> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().capacityCubicMeters() + " m³"));

        TableColumn<VehicleDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<VehicleDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(numberCol, driverCol, capacityCol, statusCol, actionsCol));
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((vehicles, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(vehicles);
            }
        }));
    }

    private void openAddDialog() {
        new VehicleFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Vehicle \"" + saved.vehicleNumber() + "\" created.");
            load();
        });
    }

    private void openEditDialog(VehicleDto vehicle) {
        new VehicleFormDialog(vehicle).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Vehicle \"" + saved.vehicleNumber() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(VehicleDto vehicle) {
        if (!ConfirmDialogs.confirmDelete("vehicle", vehicle.vehicleNumber())) {
            return;
        }
        apiClient.delete(vehicle.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Vehicle \"" + vehicle.vehicleNumber() + "\" deleted.");
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
