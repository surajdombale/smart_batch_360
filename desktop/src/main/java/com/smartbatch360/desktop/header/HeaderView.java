package com.smartbatch360.desktop.header;

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

/**
 * Header Management list screen: table + Add/Edit/Delete wired to the REST API.
 * Header stores the company/plant letterhead used on printed Batch Logs and
 * Order/Recipe reports once those (out-of-scope) modules exist.
 */
public class HeaderView {

    private final HeaderApiClient apiClient = new HeaderApiClient();
    private final CrudListView<HeaderDto> listView =
            new CrudListView<>("Header Management", "Manage company/plant letterheads used on printed reports.", "+ Add Header");

    public HeaderView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<HeaderDto> table = listView.getTable();

        TableColumn<HeaderDto, String> companyCol = new TableColumn<>("Company Name");
        companyCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().companyName()));

        TableColumn<HeaderDto, String> plantCol = new TableColumn<>("Plant/Branch Name");
        plantCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().plantName()));

        TableColumn<HeaderDto, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cd -> new SimpleStringProperty(emptyIfNull(cd.getValue().phone())));

        TableColumn<HeaderDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<HeaderDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(companyCol, plantCol, phoneCol, statusCol, actionsCol));
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((headers, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(headers);
            }
        }));
    }

    private void openAddDialog() {
        new HeaderFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Header \"" + saved.companyName() + "\" created.");
            load();
        });
    }

    private void openEditDialog(HeaderDto header) {
        new HeaderFormDialog(header).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Header \"" + saved.companyName() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(HeaderDto header) {
        if (!ConfirmDialogs.confirmDelete("header", header.companyName() + " - " + header.plantName())) {
            return;
        }
        apiClient.delete(header.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Header \"" + header.companyName() + "\" deleted.");
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
