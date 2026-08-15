package com.smartbatch360.desktop.customer;

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

/** Customer Management list screen: table + Add/Edit/Delete wired to the REST API. */
public class CustomerView {

    private final CustomerApiClient apiClient = new CustomerApiClient();
    private final CrudListView<CustomerDto> listView =
            new CrudListView<>("Customer Management", "Manage customers and their projects.", "+ Add Customer");

    public CustomerView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<CustomerDto> table = listView.getTable();

        TableColumn<CustomerDto, String> nameCol = new TableColumn<>("Customer Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));

        TableColumn<CustomerDto, String> contactCol = new TableColumn<>("Contact Person");
        contactCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().contactPerson()));

        TableColumn<CustomerDto, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().phone()));

        TableColumn<CustomerDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<CustomerDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(nameCol, contactCol, phoneCol, statusCol, actionsCol));
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((customers, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(customers);
            }
        }));
    }

    private void openAddDialog() {
        new CustomerFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Customer \"" + saved.name() + "\" created.");
            load();
        });
    }

    private void openEditDialog(CustomerDto customer) {
        new CustomerFormDialog(customer).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Customer \"" + saved.name() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(CustomerDto customer) {
        if (!ConfirmDialogs.confirmDelete("customer", customer.name())) {
            return;
        }
        apiClient.delete(customer.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Customer \"" + customer.name() + "\" deleted.");
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
