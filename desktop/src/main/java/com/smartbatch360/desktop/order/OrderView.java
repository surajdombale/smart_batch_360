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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Orders list screen: create, move through the lifecycle
 * (start / fulfil / cancel), inspect projected material consumption, delete.
 *
 * Like Production, the Actions column shows only the steps that are legal
 * from the row's current status rather than a wall of disabled buttons - the
 * backend enforces the same rules, so this is presentation, not the guard.
 * There is no Edit: an order's terms are fixed once placed; cancel and
 * re-create instead.
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
        column.setMinWidth(320);
        column.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(6);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                OrderDto order = getTableView().getItems().get(getIndex());
                box.getChildren().setAll(actionsFor(order));
                setGraphic(box);
            }
        });
        return column;
    }

    /** Only the transitions that are legal from this order's current status. */
    private List<Button> actionsFor(OrderDto order) {
        List<Button> buttons = new ArrayList<>();
        switch (order.status()) {
            case UNFULFILLED -> {
                buttons.add(lifecycleButton("Start", "button-primary", apiClient::start, order));
                buttons.add(lifecycleButton("Cancel", "button-secondary", apiClient::cancel, order));
            }
            case IN_PROGRESS -> {
                buttons.add(lifecycleButton("Fulfil", "button-primary", apiClient::fulfil, order));
                buttons.add(lifecycleButton("Cancel", "button-secondary", apiClient::cancel, order));
            }
            case FULFILLED, CANCELLED -> {
                // Terminal - nothing left to do but look at it.
            }
        }

        Button consumption = new Button("Consumption");
        consumption.getStyleClass().add("button-secondary");
        consumption.setOnAction(e -> OrderConsumptionDialog.show(order.id()));
        buttons.add(consumption);

        // An in-progress order is history; the backend refuses to delete it.
        if (order.status() != OrderStatus.IN_PROGRESS) {
            Button delete = new Button("Delete");
            delete.getStyleClass().add("button-danger");
            delete.setOnAction(e -> confirmAndDelete(order));
            buttons.add(delete);
        }
        return buttons;
    }

    private Button lifecycleButton(String label, String styleClass,
                                    Function<Long, CompletableFuture<OrderDto>> action, OrderDto order) {
        Button button = new Button(label);
        button.getStyleClass().add(styleClass);
        button.setOnAction(e -> action.apply(order.id()).whenComplete((result, throwable) ->
                Platform.runLater(() -> {
                    if (throwable != null) {
                        listView.getBanner().showError(errorMessage(throwable));
                    } else {
                        listView.getBanner().showSuccess(
                                "Order #" + result.id() + " is now " + result.status() + ".");
                    }
                    load();
                })));
        return button;
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
