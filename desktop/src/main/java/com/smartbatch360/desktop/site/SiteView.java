package com.smartbatch360.desktop.site;

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

/** Site Management list screen: table + Add/Edit/Delete wired to the REST API. */
public class SiteView {

    private final SiteApiClient apiClient = new SiteApiClient();
    private final CrudListView<SiteDto> listView =
            new CrudListView<>("Site Management", "Manage sites and project locations.", "+ Add Site");

    public SiteView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<SiteDto> table = listView.getTable();

        TableColumn<SiteDto, String> nameCol = new TableColumn<>("Site Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));

        TableColumn<SiteDto, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().customerName()));

        TableColumn<SiteDto, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().location()));

        TableColumn<SiteDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<SiteDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(nameCol, customerCol, locationCol, statusCol, actionsCol));
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((sites, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(sites);
            }
        }));
    }

    private void openAddDialog() {
        new SiteFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Site \"" + saved.name() + "\" created.");
            load();
        });
    }

    private void openEditDialog(SiteDto site) {
        new SiteFormDialog(site).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Site \"" + saved.name() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(SiteDto site) {
        if (!ConfirmDialogs.confirmDelete("site", site.name())) {
            return;
        }
        apiClient.delete(site.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Site \"" + site.name() + "\" deleted.");
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
