package com.smartbatch360.desktop.order;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.ConfirmDialogs;
import com.smartbatch360.desktop.common.CrudListView;
import com.smartbatch360.desktop.materialconsumption.OrderConsumptionDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.List;

/**
 * Orders list screen. Only creation and the UNFULFILLED state exist for now -
 * the rest of the order lifecycle is deliberately not built yet (user scope
 * decision 2026-08-27), so there is no Edit action, just Create/Delete and a
 * per-order material consumption view.
 */
public class OrderView {

    private final OrderApiClient apiClient = new OrderApiClient();
    private final CrudListView<OrderDto> listView = new CrudListView<>(
            "Orders", "Create and review sales orders.", "+ Create Order",
            o -> String.join(" ", String.valueOf(o.id()), o.clientName(), o.siteName(),
                    o.recipeName(), o.status().name()));

    public OrderView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openCreateDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<OrderDto> table = listView.getTable();

        TableColumn<OrderDto, String> idCol = new TableColumn<>("Order #");
        idCol.setCellValueFactory(cd -> new SimpleStringProperty("#" + cd.getValue().id()));

        TableColumn<OrderDto, String> clientCol = new TableColumn<>("Customer");
        clientCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().clientName()));

        TableColumn<OrderDto, String> siteCol = new TableColumn<>("Site");
        siteCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().siteName()));

        TableColumn<OrderDto, String> recipeCol = new TableColumn<>("Recipe");
        recipeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().recipeName()));

        TableColumn<OrderDto, String> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().quantityM3().toPlainString() + " m³"));

        TableColumn<OrderDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        table.getColumns().setAll(List.of(idCol, clientCol, siteCol, recipeCol, quantityCol, statusCol,
                buildActionsColumn()));
    }

    private TableColumn<OrderDto, Void> buildActionsColumn() {
        TableColumn<OrderDto, Void> column = new TableColumn<>("Actions");
        column.setSortable(false);
        column.setMinWidth(200);
        column.setCellFactory(col -> new TableCell<>() {
            private final Button consumptionButton = new Button("Consumption");
            private final Button deleteButton = new Button("Delete");
            private final HBox box = new HBox(6, consumptionButton, deleteButton);

            {
                consumptionButton.getStyleClass().add("button-secondary");
                deleteButton.getStyleClass().add("button-danger");
                consumptionButton.setOnAction(e -> OrderConsumptionDialog.show(rowItem().id()));
                deleteButton.setOnAction(e -> confirmAndDelete(rowItem()));
            }

            private OrderDto rowItem() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        return column;
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((orders, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(orders);
            }
        }));
    }

    private void openCreateDialog() {
        new OrderFormDialog().showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Order #" + saved.id() + " created (" + saved.status() + ").");
            load();
        });
    }

    private void confirmAndDelete(OrderDto order) {
        if (!ConfirmDialogs.confirmDelete("order", "#" + order.id())) {
            return;
        }
        apiClient.delete(order.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Order #" + order.id() + " deleted.");
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
