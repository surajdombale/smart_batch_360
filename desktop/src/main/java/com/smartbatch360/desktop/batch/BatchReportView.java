package com.smartbatch360.desktop.batch;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.client.ClientApiClient;
import com.smartbatch360.desktop.client.ClientDto;
import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.common.PageHeader;
import com.smartbatch360.desktop.driver.DriverApiClient;
import com.smartbatch360.desktop.driver.DriverDto;
import com.smartbatch360.desktop.recipe.RecipeApiClient;
import com.smartbatch360.desktop.recipe.RecipeDto;
import com.smartbatch360.desktop.site.SiteApiClient;
import com.smartbatch360.desktop.site.SiteDto;
import com.smartbatch360.desktop.vehicle.VehicleApiClient;
import com.smartbatch360.desktop.vehicle.VehicleDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Batch Reports: search/filter/review historical production
 * (docs/02_UI_REFERENCE.md). Scope confirmed with the user (2026-08-24):
 * search/filter/list only - PDF/Excel/Print export deliberately deferred
 * (docs/03_ARCHITECTURE.md's "do not add reporting/PDF dependencies
 * prematurely").
 */
public class BatchReportView {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());

    private final BatchReportApiClient reportApiClient = new BatchReportApiClient();
    private final ClientApiClient clientApiClient = new ClientApiClient();
    private final SiteApiClient siteApiClient = new SiteApiClient();
    private final VehicleApiClient vehicleApiClient = new VehicleApiClient();
    private final DriverApiClient driverApiClient = new DriverApiClient();
    private final RecipeApiClient recipeApiClient = new RecipeApiClient();

    private final BorderPane root = new BorderPane();
    private final NotificationBanner banner = new NotificationBanner();
    private final StackPane centerStack = new StackPane();
    private final TableView<BatchDto> table = new TableView<>();

    private final TextField batchNumberFromField = new TextField();
    private final TextField batchNumberToField = new TextField();
    private final DatePicker dateFromField = new DatePicker();
    private final DatePicker dateToField = new DatePicker();
    private final ComboBox<ClientDto> clientField = new ComboBox<>();
    private final ComboBox<SiteDto> siteField = new ComboBox<>();
    private final ComboBox<VehicleDto> vehicleField = new ComboBox<>();
    private final ComboBox<DriverDto> driverField = new ComboBox<>();
    private final ComboBox<RecipeDto> recipeField = new ComboBox<>();

    private final Label pageLabel = new Label();
    private final Button previousButton = new Button("‹ Previous");
    private final Button nextButton = new Button("Next ›");

    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    public BatchReportView() {
        PageHeader header = new PageHeader("Batch Reports", "Search, filter and review historical production batches.");

        VBox top = new VBox(10, header, banner, buildFilterPanel());
        root.setTop(top);
        root.setCenter(centerStack);
        root.setBottom(buildPaginationBar());
        root.getStyleClass().add("content-area");

        setupTable();
        loadReferenceLists();
        search();
    }

    private VBox buildFilterPanel() {
        clientField.setPromptText("All Clients");
        siteField.setPromptText("All Sites");
        vehicleField.setPromptText("All Vehicles");
        driverField.setPromptText("All Drivers");
        recipeField.setPromptText("All Recipes");
        batchNumberFromField.setPromptText("From");
        batchNumberToField.setPromptText("To");
        batchNumberFromField.setPrefWidth(110);
        batchNumberToField.setPrefWidth(110);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.addRow(0,
                labeled("Batch Number", new HBox(6, batchNumberFromField, batchNumberToField)),
                labeled("Date From", dateFromField),
                labeled("Date To", dateToField));

        grid.addRow(1,
                labeled("Client", clientField),
                labeled("Site", siteField),
                labeled("Vehicle", vehicleField));

        grid.addRow(2,
                labeled("Driver", driverField),
                labeled("Recipe", recipeField));

        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("button-primary");
        searchButton.setOnAction(e -> { currentPage = 0; search(); });

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("button-secondary");
        resetButton.setOnAction(e -> resetFilters());

        HBox actions = new HBox(10, searchButton, resetButton);
        actions.setPadding(new Insets(8, 0, 0, 0));

        VBox panel = new VBox(8, grid, actions);
        panel.getStyleClass().add("card");
        return panel;
    }

    private VBox labeled(String label, javafx.scene.Node control) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("form-label");
        return new VBox(4, labelNode, control);
    }

    private HBox buildPaginationBar() {
        previousButton.getStyleClass().add("button-secondary");
        nextButton.getStyleClass().add("button-secondary");
        previousButton.setOnAction(e -> { currentPage--; search(); });
        nextButton.setOnAction(e -> { currentPage++; search(); });

        HBox bar = new HBox(12, previousButton, pageLabel, nextButton);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 0, 0, 0));
        return bar;
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No batches match these filters."));

        TableColumn<BatchDto, String> numberCol = new TableColumn<>("Batch Number");
        numberCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().batchNumber()));

        TableColumn<BatchDto, String> dateCol = new TableColumn<>("Cycle Date/Time");
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().cycleDateTime() != null ? TIMESTAMP_FORMAT.format(cd.getValue().cycleDateTime()) : ""));

        TableColumn<BatchDto, String> recipeCol = new TableColumn<>("Recipe");
        recipeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().recipeName()));

        TableColumn<BatchDto, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().clientName()));

        TableColumn<BatchDto, String> siteCol = new TableColumn<>("Site");
        siteCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().siteName()));

        TableColumn<BatchDto, String> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().vehicleNumber()));

        TableColumn<BatchDto, String> driverCol = new TableColumn<>("Driver");
        driverCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().driverName()));

        TableColumn<BatchDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status().name()));

        TableColumn<BatchDto, Void> viewCol = new TableColumn<>("");
        viewCol.setSortable(false);
        viewCol.setMinWidth(70);
        viewCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");

            {
                viewButton.getStyleClass().add("button-secondary");
                viewButton.setOnAction(e -> BatchDetailDialog.show(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });

        table.getColumns().setAll(List.of(numberCol, dateCol, recipeCol, clientCol, siteCol, vehicleCol, driverCol, statusCol, viewCol));
    }

    private void loadReferenceLists() {
        clientApiClient.list().whenComplete((items, t) -> Platform.runLater(() ->
                clientField.setItems(FXCollections.observableArrayList(t == null ? items : List.of()))));
        siteApiClient.list().whenComplete((items, t) -> Platform.runLater(() ->
                siteField.setItems(FXCollections.observableArrayList(t == null ? items : List.of()))));
        vehicleApiClient.list().whenComplete((items, t) -> Platform.runLater(() ->
                vehicleField.setItems(FXCollections.observableArrayList(t == null ? items : List.of()))));
        driverApiClient.list().whenComplete((items, t) -> Platform.runLater(() ->
                driverField.setItems(FXCollections.observableArrayList(t == null ? items : List.of()))));
        recipeApiClient.list().whenComplete((items, t) -> Platform.runLater(() ->
                recipeField.setItems(FXCollections.observableArrayList(t == null ? items : List.of()))));
    }

    private void resetFilters() {
        batchNumberFromField.clear();
        batchNumberToField.clear();
        dateFromField.setValue(null);
        dateToField.setValue(null);
        clientField.getSelectionModel().clearSelection();
        siteField.getSelectionModel().clearSelection();
        vehicleField.getSelectionModel().clearSelection();
        driverField.getSelectionModel().clearSelection();
        recipeField.getSelectionModel().clearSelection();
        currentPage = 0;
        search();
    }

    private void search() {
        centerStack.getChildren().setAll(loadingIndicator());

        BatchReportFilter filter = new BatchReportFilter(
                blankToNull(batchNumberFromField.getText()), blankToNull(batchNumberToField.getText()),
                dateFromField.getValue(), dateToField.getValue(),
                idOf(clientField.getValue(), ClientDto::id), idOf(siteField.getValue(), SiteDto::id),
                idOf(vehicleField.getValue(), VehicleDto::id), idOf(driverField.getValue(), DriverDto::id),
                idOf(recipeField.getValue(), RecipeDto::id));

        reportApiClient.search(filter, currentPage, PAGE_SIZE).whenComplete((result, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                showError(throwable);
            } else {
                showResults(result);
            }
        }));
    }

    private <T> Long idOf(T item, java.util.function.Function<T, Long> idExtractor) {
        return item == null ? null : idExtractor.apply(item);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProgressIndicator loadingIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(48, 48);
        return indicator;
    }

    private void showError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause instanceof ApiException apiEx ? apiEx.getMessage() : "Something went wrong. Please try again.";

        VBox box = new VBox(8);
        box.getStyleClass().addAll("state-container", "state-error");
        box.setAlignment(Pos.CENTER);
        Label title = new Label("Unable to load batch reports");
        title.getStyleClass().add("state-title");
        Label msg = new Label(message);
        msg.getStyleClass().add("state-message");
        msg.setWrapText(true);
        Button retry = new Button("Retry");
        retry.getStyleClass().add("button-secondary");
        retry.setOnAction(e -> search());
        box.getChildren().addAll(title, msg, retry);
        centerStack.getChildren().setAll(box);

        pageLabel.setText("");
        previousButton.setDisable(true);
        nextButton.setDisable(true);
    }

    private void showResults(BatchPageDto result) {
        table.setItems(FXCollections.observableArrayList(result.content()));
        centerStack.getChildren().setAll(table);

        long shownFrom = result.totalElements() == 0 ? 0 : (long) result.page() * result.size() + 1;
        long shownTo = Math.min((long) (result.page() + 1) * result.size(), result.totalElements());
        pageLabel.setText(String.format("Showing %d-%d of %d (page %d of %d)",
                shownFrom, shownTo, result.totalElements(), result.page() + 1, Math.max(result.totalPages(), 1)));

        previousButton.setDisable(result.page() <= 0);
        nextButton.setDisable(result.page() + 1 >= result.totalPages());
    }

    public Region getView() {
        return root;
    }
}
