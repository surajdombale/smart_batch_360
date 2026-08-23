package com.smartbatch360.desktop.dashboard;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.PageHeader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.*;

/**
 * Phase 1 dashboard: only KPIs backed by data that actually exists in this
 * phase (Client/Site/Vehicle/Driver counts, Backend/Database/API status).
 * Batches, Revenue, Production Trend, Recipes, PLC status, Recent Activity,
 * Active Recipe, Last Batch and Average Batch Time are intentionally omitted -
 * their data sources (Production/Recipe/BatchData) are out of scope for
 * Phase 1 (docs/01_REQUIREMENTS.md, docs/02_UI_REFERENCE.md).
 */
public class DashboardView extends BorderPane {

    private final DashboardApiClient apiClient = new DashboardApiClient();
    private final StackPane centerStack = new StackPane();
    private final GridPane kpiGrid = new GridPane();
    private final GridPane statusGrid = new GridPane();

    public DashboardView() {
        PageHeader header = new PageHeader("Dashboard", "Real-time overview of plant performance and key metrics.");

        VBox top = new VBox(header);
        setTop(top);
        setCenter(centerStack);
        getStyleClass().add("content-area");

        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);
        statusGrid.setHgap(16);
        statusGrid.setVgap(16);

        load();
    }

    private void load() {
        centerStack.getChildren().setAll(loadingIndicator());

        apiClient.getSummary()
                .whenComplete((summary, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        showError(throwable);
                    } else {
                        showSummary(summary);
                    }
                }));
    }

    private ProgressIndicator loadingIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(48, 48);
        return indicator;
    }

    private void showError(Throwable throwable) {
        String message = throwable.getCause() instanceof ApiException apiEx
                ? apiEx.getMessage()
                : "Could not load dashboard data. Check that the SmartBatch360 backend is running.";

        VBox box = new VBox(8);
        box.getStyleClass().add("state-container");
        box.setAlignment(Pos.CENTER);
        Label title = new Label("Unable to load dashboard");
        title.getStyleClass().add("state-title");
        Label msg = new Label(message);
        msg.getStyleClass().add("state-message");
        msg.setWrapText(true);
        Button retry = new Button("Retry");
        retry.getStyleClass().add("button-secondary");
        retry.setOnAction(e -> load());
        box.getChildren().addAll(title, msg, retry);
        centerStack.getChildren().setAll(box);
    }

    private void showSummary(DashboardSummaryDto summary) {
        kpiGrid.getChildren().clear();
        kpiGrid.add(kpiCard("Total Clients", String.valueOf(summary.totalClients())), 0, 0);
        kpiGrid.add(kpiCard("Total Sites", String.valueOf(summary.totalSites())), 1, 0);
        kpiGrid.add(kpiCard("Total Vehicles", String.valueOf(summary.totalVehicles())), 2, 0);
        kpiGrid.add(kpiCard("Total Drivers", String.valueOf(summary.totalDrivers())), 3, 0);
        kpiGrid.add(kpiCard("Total Headers", String.valueOf(summary.totalHeaders())), 4, 0);
        kpiGrid.add(kpiCard("Total Recipes", String.valueOf(summary.totalRecipes())), 5, 0);

        statusGrid.getChildren().clear();
        statusGrid.add(statusCard("Backend", summary.backendStatus()), 0, 0);
        statusGrid.add(statusCard("Database", summary.databaseStatus()), 1, 0);
        statusGrid.add(statusCard("API", summary.apiStatus()), 2, 0);

        Label statusHeading = new Label("Plant Status");
        statusHeading.getStyleClass().add("page-header-subtitle");

        VBox content = new VBox(20, kpiGrid, statusHeading, statusGrid);
        content.setPadding(new Insets(4, 0, 0, 0));
        centerStack.getChildren().setAll(content);
    }

    private VBox kpiCard(String label, String value) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPrefWidth(180);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");
        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("kpi-label");
        card.getChildren().addAll(nameLabel, valueLabel);
        return card;
    }

    private VBox statusCard(String label, String status) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPrefWidth(180);
        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("kpi-label");
        Label statusLabel = new Label(status == null ? "UNKNOWN" : status);
        statusLabel.getStyleClass().add("UP".equals(status) ? "status-up" : "status-down");
        card.getChildren().addAll(nameLabel, statusLabel);
        return card;
    }
}
