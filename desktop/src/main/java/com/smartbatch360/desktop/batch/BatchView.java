package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.ActionsColumn;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Production list screen: real Batch CRUD (docs/06_SCOPE_AND_ROADMAP.md,
 * scope confirmed with the user 2026-08-23) plus manual/simulated controls
 * (Start/Pause/Resume/Stop/Emergency Stop) - no PLC integration. The
 * Controls column shows only the buttons relevant to a batch's current
 * status rather than a wall of disabled buttons.
 */
public class BatchView {

    private final BatchApiClient apiClient = new BatchApiClient();
    private final CrudListView<BatchDto> listView = new CrudListView<>(
            "Production", "Manage production batches.", "+ Add Batch",
            b -> String.join(" ", b.batchNumber(), b.recipeName(), b.clientName(), b.siteName(),
                    b.vehicleNumber(), b.driverName(), b.status().name()));

    public BatchView() {
        setupColumns();
        listView.getAddButton().setOnAction(e -> openAddDialog());
        listView.getRefreshButton().setOnAction(e -> load());
        listView.setOnRetry(this::load);
        load();
    }

    private void setupColumns() {
        TableView<BatchDto> table = listView.getTable();

        TableColumn<BatchDto, String> numberCol = new TableColumn<>("Batch Number");
        numberCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().batchNumber()));

        TableColumn<BatchDto, String> recipeCol = new TableColumn<>("Recipe");
        recipeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().recipeName()));

        TableColumn<BatchDto, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().clientName()));

        TableColumn<BatchDto, String> quantityCol = new TableColumn<>("Target / Produced");
        quantityCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().targetQuantity() + " / " + cd.getValue().producedQuantity() + " m³"));

        TableColumn<BatchDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<BatchDto, Void> controlsCol = buildControlsColumn();
        TableColumn<BatchDto, Void> actionsCol = ActionsColumn.create(this::openEditDialog, this::confirmAndDelete);

        table.getColumns().setAll(List.of(numberCol, recipeCol, clientCol, quantityCol, statusCol, controlsCol, actionsCol));
    }

    private TableColumn<BatchDto, Void> buildControlsColumn() {
        TableColumn<BatchDto, Void> column = new TableColumn<>("Controls");
        column.setSortable(false);
        column.setMinWidth(260);

        column.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(4);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                BatchDto batch = getTableView().getItems().get(getIndex());
                box.getChildren().setAll(controlButtonsFor(batch));
                setGraphic(box);
            }
        });
        return column;
    }

    private List<Button> controlButtonsFor(BatchDto batch) {
        return switch (batch.status()) {
            case PENDING -> List.of(
                    controlButton("Start", "button-primary", apiClient::start, batch));
            case IN_PROGRESS -> List.of(
                    controlButton("Pause", "button-secondary", apiClient::pause, batch),
                    controlButton("Stop", "button-secondary", apiClient::stop, batch),
                    controlButton("E-Stop", "button-danger", apiClient::emergencyStop, batch));
            case PAUSED -> List.of(
                    controlButton("Resume", "button-primary", apiClient::resume, batch),
                    controlButton("Stop", "button-secondary", apiClient::stop, batch),
                    controlButton("E-Stop", "button-danger", apiClient::emergencyStop, batch));
            case STOPPED, COMPLETED -> List.of();
        };
    }

    private Button controlButton(String label, String styleClass, Function<Long, CompletableFuture<BatchDto>> action, BatchDto batch) {
        Button button = new Button(label);
        button.getStyleClass().add(styleClass);
        button.setOnAction(e -> runControl(action, batch, label));
        return button;
    }

    private void runControl(Function<Long, CompletableFuture<BatchDto>> action, BatchDto batch, String label) {
        action.apply(batch.id()).whenComplete((updated, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Batch \"" + batch.batchNumber() + "\": " + label + " applied.");
                load();
            }
        }));
    }

    private void load() {
        listView.showLoading();
        apiClient.list().whenComplete((batches, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.showError(errorMessage(throwable));
            } else {
                listView.showData(batches);
            }
        }));
    }

    private void openAddDialog() {
        new BatchFormDialog(null).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Batch \"" + saved.batchNumber() + "\" created.");
            load();
        });
    }

    private void openEditDialog(BatchDto batch) {
        new BatchFormDialog(batch).showAndWait().ifPresent(saved -> {
            listView.getBanner().showSuccess("Batch \"" + saved.batchNumber() + "\" updated.");
            load();
        });
    }

    private void confirmAndDelete(BatchDto batch) {
        if (!ConfirmDialogs.confirmDelete("batch", batch.batchNumber())) {
            return;
        }
        apiClient.delete(batch.id()).whenComplete((v, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                listView.getBanner().showError(errorMessage(throwable));
            } else {
                listView.getBanner().showSuccess("Batch \"" + batch.batchNumber() + "\" deleted.");
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
