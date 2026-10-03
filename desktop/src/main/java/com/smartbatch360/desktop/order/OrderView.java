package com.smartbatch360.desktop.order;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.ConfirmDialogs;
import com.smartbatch360.desktop.common.CrudListView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Orders list screen: create, move through the lifecycle
 * (start / fulfil / cancel), inspect projected material consumption, delete.
 *
 * The Actions column carries Edit and Delete. The lifecycle buttons and the
 * Status column were removed on 2026-10-03 at the user's request: fulfilment is
 * what the screen is for, and the Produced column measures that from the
 * order's batches. The lifecycle endpoints still exist on the API.
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

        // Produced against ordered, in one column. Separate Quantity/Produced/
        // Remaining columns were tried first and made the table unreadable:
        // nine columns share a fixed width, so every one of them collapsed to
        // an ellipsis ("10.000...", "UNFULFI...") and the figures this pass
        // exists to show could not be read at all. Remaining is just the
        // subtraction of the two, so it stays off the list; the API still
        // returns it for anything that wants the figure directly.
        TableColumn<OrderDto, String> producedCol = new TableColumn<>("Produced");
        producedCol.setCellValueFactory(cd -> new SimpleStringProperty(
                trim(cd.getValue().producedQuantityKg()) + " of " + trim(cd.getValue().quantityKg()) + " kg"));

        // This one table opts out of the shared CONSTRAINED policy. That
        // policy divides the width by column count and honours neither
        // prefWidth nor, in the end, the Actions minimum, so with seven
        // columns and a four-button Actions cell something was always
        // clipped: pin one column and the ellipsis simply moved to the next,
        // and Actions ended up too narrow to reach the last buttons.
        // Sizing the columns explicitly fits them all, and if the window is
        // ever too narrow the overflow becomes a scrollbar you can use rather
        // than buttons off the edge.
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        sizeColumn(idCol, 55);
        sizeColumn(clientCol, 72);
        sizeColumn(siteCol, 60);
        sizeColumn(recipeCol, 90);
        sizeColumn(producedCol, 78);

        table.getColumns().setAll(List.of(idCol, clientCol, siteCol, recipeCol,
                producedCol, buildActionsColumn()));
    }

    /**
     * Fixes a data column at one width. prefWidth on its own did not hold -
     * the columns still came out far wider and pushed Actions off the end -
     * so max is set with it, which does.
     */
    private static void sizeColumn(TableColumn<OrderDto, ?> column, double width) {
        column.setPrefWidth(width);
        column.setMaxWidth(width);
    }

    private TableColumn<OrderDto, Void> buildActionsColumn() {
        TableColumn<OrderDto, Void> column = new TableColumn<>("Actions");
        column.setSortable(false);
        // Wide enough for Edit and Delete. These are layout units, not screen
        // pixels - on a 150% display each one paints as 1.5px, which is what
        // made an earlier set of widths measured off a screenshot half again
        // too large for the table.
        column.setMinWidth(160);
        column.setPrefWidth(160);
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

    /**
     * Edit and Delete. The lifecycle buttons (Start, Fulfil, Cancel) and the
     * Consumption button were removed on 2026-10-03 at the user's request; the
     * Status column went with them. What an order has actually had produced
     * against it is in the Produced column, measured from its batches, which is
     * the figure the screen is for.
     */
    private List<Button> actionsFor(OrderDto order) {
        List<Button> buttons = new ArrayList<>();

        Button edit = new Button("Edit");
        edit.getStyleClass().add("button-secondary");
        edit.setOnAction(e -> openEditDialog(order));
        buttons.add(edit);

        // An in-progress order is history; the backend refuses to delete it.
        if (order.status() != OrderStatus.IN_PROGRESS) {
            Button delete = new Button("Delete");
            delete.getStyleClass().add("button-danger");
            delete.setOnAction(e -> confirmAndDelete(order));
            buttons.add(delete);
        }
        // Let the buttons keep their own width. An HBox will otherwise shrink
        // them to fit the column and ellipsise the labels, which is worse than
        // the table scrolling to reach them.
        buttons.forEach(button -> button.setMinWidth(Region.USE_PREF_SIZE));
        return buttons;
    }

    private void openEditDialog(OrderDto order) {
        new OrderFormDialog(order).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Order #" + saved.id() + " updated.");
            load();
        });
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

    /**
     * Quantities are DECIMAL(12,4), so an ordered 10 arrives as "10.0000" and
     * three of those per row is what overflowed the table. The scale carries
     * no information here - drop it and keep any digits that are real.
     */
    private static String trim(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String errorMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause instanceof ApiException apiEx ? apiEx.getMessage() : "Something went wrong. Please try again.";
    }

    public Region getView() {
        return listView.getView();
    }
}
