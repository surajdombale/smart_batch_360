package com.smartbatch360.desktop.client;

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

/** Client Management list screen (originally "Customer"): table + Add/Edit/Delete wired to the REST API. */
public class ClientView {

    private final ClientApiClient apiClient = new ClientApiClient();
    private final CrudListView<ClientDto> listView = new CrudListView<>(
            "Client Management", "Manage clients and their projects.", "+ Add Client",
            c -> String.join(" ", c.name(), c.contactPerson(), c.phone(),
                    emptyIfNull(c.address()), c.status().name()));

    public ClientView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<ClientDto> table = listView.getTable();

        TableColumn<ClientDto, String> nameCol = new TableColumn<>("Client Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));

        TableColumn<ClientDto, String> contactCol = new TableColumn<>("Contact Person");
        contactCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().contactPerson()));

        TableColumn<ClientDto, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().phone()));

        TableColumn<ClientDto, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(cd -> new SimpleStringProperty(emptyIfNull(cd.getValue().address())));

        TableColumn<ClientDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<ClientDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(nameCol, contactCol, phoneCol, addressCol, statusCol, actionsCol));
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((clients, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(clients);
            }
        }));
    }

    private void openAddDialog() {
        new ClientFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Client \"" + saved.name() + "\" created.");
            load();
        });
    }

    private void openEditDialog(ClientDto client) {
        new ClientFormDialog(client).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Client \"" + saved.name() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(ClientDto client) {
        if (!ConfirmDialogs.confirmDelete("client", client.name())) {
            return;
        }
        apiClient.delete(client.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Client \"" + client.name() + "\" deleted.");
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
