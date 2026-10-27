package com.smartbatch360.desktop.dashboard;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.batch.BatchStatus;
import com.smartbatch360.desktop.dashboard.DashboardProductionDto.RecentBatch;
import com.smartbatch360.desktop.dashboard.DashboardProductionDto.Totals;
import com.smartbatch360.desktop.dashboard.DashboardProductionDto.TrendPoint;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The dashboard, laid out after the SmartBatch360 v2.0 reference the user
 * supplied on 27-Oct-2026: a period picker and refresh at the top, coloured
 * headline figures, the batch trend beside the latest batches, and the
 * connection status along the bottom.
 *
 * Every figure comes from data the app holds. The reference's Revenue tile is
 * left out because nothing records a price, and PLC shows as not monitored
 * because there is no live PLC link to watch (docs/06_SCOPE_AND_ROADMAP.md).
 *
 * It refreshes every five seconds while on screen, as the requirements ask,
 * and updates in place rather than rebuilding, so a refresh does not reset the
 * page under someone reading it.
 */
public class DashboardView extends BorderPane {

    private static final Duration REFRESH_INTERVAL = Duration.seconds(5);

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DAY_AND_TIME =
            DateTimeFormatter.ofPattern("d MMM, hh:mm a").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter AXIS_DAY = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter TOOLTIP_DAY = DateTimeFormatter.ofPattern("EEE d MMM");

    private static final DecimalFormat COUNT = new DecimalFormat("#,##0");
    private static final DecimalFormat VOLUME = new DecimalFormat("#,##0.00");

    // Icon outlines on a 24x24 grid.
    private static final String ICON_DASHBOARD = "M3 3h8v10H3z M13 3h8v6h-8z M13 11h8v10h-8z M3 15h8v6H3z";
    private static final String ICON_REFRESH = "M23 4v6h-6 M1 20v-6h6 M3.51 9a9 9 0 0 1 14.85-3.36L23 10 "
            + "M1 14l4.64 4.36A9 9 0 0 0 20.49 15";
    private static final String ICON_BATCHES = "M9 2h6a1 1 0 0 1 1 1v1h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6"
            + "a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2V3a1 1 0 0 1 1-1z M8 10h8v1.6H8z M8 14h8v1.6H8z M8 18h5v1.6H8z";
    private static final String ICON_VOLUME = "M12 2.5c0 0-7 7.6-7 12.5a7 7 0 0 0 14 0c0-4.9-7-12.5-7-12.5z";
    private static final String ICON_VEHICLE = "M1 5h13v11H1z M14 9h4.5L22 12.5V16h-8z "
            + "M5.5 20.5a2 2 0 1 0 0-4a2 2 0 1 0 0 4z M17.5 20.5a2 2 0 1 0 0-4a2 2 0 1 0 0 4z";

    /** Column widths shared by the header and every row, so they line up. */
    private static final double[] RECENT_COLUMNS = {14, 15, 17, 14, 22, 18};
    private static final int QUANTITY_COLUMN = 3;

    /**
     * Narrower than this, the chart and the recent batches stack. Side by side
     * in the app's default 1000px window, six columns had about 55px each and
     * the headings ran into one another.
     */
    private static final double STACK_PANELS_BELOW = 860;

    private final DashboardApiClient apiClient = new DashboardApiClient();
    private final ComboBox<DashboardPeriod> periodField = new ComboBox<>();
    private final StackPane centerStack = new StackPane();
    private final Timeline refreshTimeline = new Timeline(new KeyFrame(REFRESH_INTERVAL, e -> refresh()));

    private final KpiTile batchesTile = new KpiTile("Total Batches", ICON_BATCHES, "kpi-tile-blue",
            "Batches made in the period, whatever their status.");
    private final KpiTile volumeTile = new KpiTile("Total Quantity", ICON_VOLUME, "kpi-tile-green",
            "Concrete produced, in m³: the kilograms weighed out, at the plant's concrete density. "
                    + "A batch still running counts what it has made so far.");
    private final KpiTile vehiclesTile = new KpiTile("Active Vehicles", ICON_VEHICLE, "kpi-tile-purple",
            "Vehicles that carried at least one batch in the period.");

    private final NumberAxis trendX = new NumberAxis();
    private final NumberAxis trendY = new NumberAxis();
    private final AreaChart<Number, Number> trendChart = new AreaChart<>(trendX, trendY);
    private final XYChart.Series<Number, Number> trendSeries = new XYChart.Series<>();
    private final Label trendSubtitle = new Label();
    private final VBox recentRows = new VBox();
    private final GridPane panels = new GridPane();
    private final VBox trendCard = card("Batch Summary", trendSubtitle, trendChart);
    private final VBox recentCard = card("Recent Batches", null, recentRows);
    private Boolean panelsStacked;

    private final StatusItem backendStatus = new StatusItem();
    private final StatusItem databaseStatus = new StatusItem();
    private final StatusItem plcStatus = new StatusItem();
    private final Label updatedLabel = new Label();

    private final VBox content;

    /** Bumped per request, so a slow answer for a period no longer selected is dropped. */
    private long requestSequence;
    private boolean requestInFlight;
    private boolean contentShown;
    private DashboardProductionDto shownProduction;
    private LocalTime lastUpdated;

    public DashboardView() {
        getStyleClass().add("content-area");
        configureTrendChart();

        content = new VBox(16, buildKpiRow(), buildPanels());
        setTop(buildHeader());
        setCenter(centerStack);
        setBottom(buildStatusBar());

        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        // Navigating away swaps in a new screen and leaves this one without a
        // scene; it must then stop asking for figures nobody can see.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                refreshTimeline.stop();
            } else {
                refreshTimeline.play();
            }
        });

        load(true);
    }

    // ---------- Layout ----------

    private HBox buildHeader() {
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        periodField.getItems().setAll(DashboardPeriod.values());
        periodField.setValue(DashboardPeriod.TODAY);
        periodField.getStyleClass().add("period-field");
        periodField.valueProperty().addListener((obs, oldPeriod, period) -> load(!contentShown));

        Button refreshButton = new Button();
        refreshButton.setGraphic(icon(ICON_REFRESH, "refresh-icon", 0.65));
        refreshButton.getStyleClass().addAll("button-secondary", "icon-button");
        refreshButton.setTooltip(new Tooltip("Refresh now"));
        refreshButton.setOnAction(e -> load(!contentShown));

        HBox header = new HBox(10, icon(ICON_DASHBOARD, "dashboard-title-icon", 0.85), title, spacer,
                periodField, refreshButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dashboard-header");
        return header;
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(16, batchesTile, volumeTile, vehiclesTile);
        row.getChildren().forEach(tile -> HBox.setHgrow(tile, Priority.ALWAYS));
        return row;
    }

    private GridPane buildPanels() {
        panels.setHgap(16);
        panels.setVgap(16);
        arrangePanels(false);
        panels.widthProperty().addListener((obs, oldWidth, width) ->
                arrangePanels(width.doubleValue() < STACK_PANELS_BELOW));
        VBox.setVgrow(panels, Priority.ALWAYS);
        return panels;
    }

    /** Side by side as in the reference, or one above the other when that would cramp the table. */
    private void arrangePanels(boolean stacked) {
        if (Boolean.valueOf(stacked).equals(panelsStacked)) {
            return;
        }
        panelsStacked = stacked;
        panels.getChildren().clear();
        RowConstraints growing = new RowConstraints();
        growing.setVgrow(Priority.ALWAYS);
        if (stacked) {
            ColumnConstraints full = new ColumnConstraints();
            full.setPercentWidth(100);
            panels.getColumnConstraints().setAll(full);
            panels.getRowConstraints().setAll(growing, new RowConstraints());
            panels.add(trendCard, 0, 0);
            panels.add(recentCard, 0, 1);
        } else {
            ColumnConstraints chartColumn = new ColumnConstraints();
            chartColumn.setPercentWidth(42);
            ColumnConstraints recentColumn = new ColumnConstraints();
            recentColumn.setPercentWidth(58);
            panels.getColumnConstraints().setAll(chartColumn, recentColumn);
            panels.getRowConstraints().setAll(growing);
            panels.add(trendCard, 0, 0);
            panels.add(recentCard, 1, 0);
        }
    }

    private static VBox card(String title, Label subtitle, Node body) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        VBox heading = new VBox(2, titleLabel);
        if (subtitle != null) {
            subtitle.getStyleClass().add("card-subtitle");
            heading.getChildren().add(subtitle);
        }
        VBox card = new VBox(12, heading, body);
        card.getStyleClass().add("card");
        card.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(body, Priority.ALWAYS);
        return card;
    }

    private HBox buildStatusBar() {
        backendStatus.show("Backend", "Connecting…", StatusItem.IDLE);
        databaseStatus.show("Database", "Connecting…", StatusItem.IDLE);
        plcStatus.show("PLC", "Not monitored", StatusItem.IDLE);
        Tooltip.install(plcStatus, new Tooltip("There is no live PLC link to monitor yet. "
                + "Cycles the PLC reports still reach the app through the API."));

        updatedLabel.getStyleClass().add("status-updated");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(20, backendStatus, databaseStatus, plcStatus, spacer, updatedLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("dashboard-status-bar");
        BorderPane.setMargin(bar, new Insets(16, 0, 0, 0));
        return bar;
    }

    private void configureTrendChart() {
        trendChart.getData().add(trendSeries);
        trendChart.getStyleClass().add("trend-chart");
        // Redrawn every five seconds - an animation each time would never settle.
        trendChart.setAnimated(false);
        // One series, named by the card's title and subtitle.
        trendChart.setLegendVisible(false);
        trendChart.setVerticalGridLinesVisible(false);
        trendChart.setMinHeight(200);
        trendChart.setPrefHeight(240);

        trendX.setAutoRanging(false);
        trendX.setMinorTickVisible(false);
        trendY.setAutoRanging(false);
        trendY.setMinorTickVisible(false);
        trendY.setTickLabelFormatter(axisLabels(value -> COUNT.format(value.longValue())));
    }

    // ---------- Loading ----------

    /** Fetches now: on opening, on a new period, or when Refresh is pressed. */
    private void load(boolean showSpinner) {
        long sequence = ++requestSequence;
        requestInFlight = true;
        if (showSpinner) {
            centerStack.getChildren().setAll(loadingIndicator());
        }
        DashboardPeriod period = periodField.getValue();

        apiClient.getSummary()
                .thenCombine(apiClient.getProduction(period),
                        (summary, production) -> (Runnable) () -> show(period, summary, production))
                .whenComplete((showIt, throwable) -> Platform.runLater(() -> {
                    if (sequence != requestSequence) {
                        return;
                    }
                    requestInFlight = false;
                    if (throwable != null) {
                        showFailure(throwable);
                    } else {
                        showIt.run();
                    }
                }));
    }

    /** The five-second tick. Skipped while a fetch is still out, so a slow server is not piled on. */
    private void refresh() {
        if (!requestInFlight) {
            load(false);
        }
    }

    private ProgressIndicator loadingIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(48, 48);
        return indicator;
    }

    private void showFailure(Throwable throwable) {
        ApiException apiException = throwable.getCause() instanceof ApiException e ? e : null;
        boolean unreachable = apiException == null || apiException.statusCode() == 0;
        backendStatus.show("Backend", unreachable ? "Not reachable" : "Error", StatusItem.DOWN);
        databaseStatus.show("Database", "Unknown", StatusItem.IDLE);

        if (contentShown) {
            // Keep the last figures up rather than blanking the page over one
            // missed refresh, but say how old they are.
            updatedLabel.setText(lastUpdated == null ? ""
                    : "Could not refresh - showing figures from " + CLOCK.format(lastUpdated));
            return;
        }

        String message = apiException != null
                ? apiException.getMessage()
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
        retry.setOnAction(e -> load(true));
        box.getChildren().addAll(title, msg, retry);
        centerStack.getChildren().setAll(box);
    }

    // ---------- Showing figures ----------

    private void show(DashboardPeriod period, DashboardSummaryDto summary, DashboardProductionDto production) {
        boolean backendUp = "UP".equals(summary.backendStatus());
        boolean databaseUp = "UP".equals(summary.databaseStatus());
        backendStatus.show("Backend", backendUp ? "Connected" : "Down", backendUp ? StatusItem.UP : StatusItem.DOWN);
        databaseStatus.show("Database", databaseUp ? "Connected" : "Disconnected",
                databaseUp ? StatusItem.UP : StatusItem.DOWN);
        lastUpdated = LocalTime.now();
        updatedLabel.setText("Updated " + CLOCK.format(lastUpdated));

        if (!contentShown) {
            centerStack.getChildren().setAll(content);
            contentShown = true;
        }
        // Most ticks change nothing; redrawing anyway would close an open tooltip.
        if (production.equals(shownProduction)) {
            return;
        }
        shownProduction = production;
        showTotals(period, production.current(), production.previous());
        showTrend(period, production.trend());
        showRecent(period, production.recentBatches());
    }

    private void showTotals(DashboardPeriod period, Totals current, Totals previous) {
        batchesTile.show(COUNT.format(current.batches()), "", DashboardFigures.change(
                BigDecimal.valueOf(current.batches()), BigDecimal.valueOf(previous.batches()), COUNT, "", period));
        volumeTile.show(VOLUME.format(current.producedM3()), "m³", DashboardFigures.change(
                current.producedM3(), previous.producedM3(), VOLUME, " m³", period));
        vehiclesTile.show(COUNT.format(current.activeVehicles()), "", DashboardFigures.change(
                BigDecimal.valueOf(current.activeVehicles()), BigDecimal.valueOf(previous.activeVehicles()),
                COUNT, "", period));
    }

    /**
     * A single day is plotted against its 24 hours, so today's line visibly
     * stops at the current hour rather than ending wherever the data does.
     */
    private void showTrend(DashboardPeriod period, List<TrendPoint> trend) {
        List<XYChart.Data<Number, Number>> points = new ArrayList<>(trend.size());
        List<String> tooltips = new ArrayList<>(trend.size());
        long peak = 0;
        for (int i = 0; i < trend.size(); i++) {
            TrendPoint point = trend.get(i);
            int x = period.singleDay() ? point.start().getHour() : i;
            points.add(new XYChart.Data<>(x, point.batches()));
            String when = period.singleDay()
                    ? HOUR.format(point.start()) + " - " + HOUR.format(point.start().plusHours(1))
                    : TOOLTIP_DAY.format(point.start());
            tooltips.add(when + "\n" + DashboardFigures.batches(point.batches()));
            peak = Math.max(peak, point.batches());
        }

        trendX.setLowerBound(0);
        if (period.singleDay()) {
            trendSubtitle.setText("Batches per hour");
            trendX.setUpperBound(24);
            trendX.setTickUnit(4);
            trendX.setTickLabelFormatter(axisLabels(value -> String.format("%02d:00", value.intValue())));
        } else {
            trendSubtitle.setText("Batches per day");
            List<LocalDate> days = trend.stream().map(point -> point.start().toLocalDate()).toList();
            trendX.setUpperBound(Math.max(1, days.size() - 1));
            trendX.setTickUnit(days.size() <= 7 ? 1 : 5);
            trendX.setTickLabelFormatter(axisLabels(value -> {
                int index = (int) Math.round(value.doubleValue());
                return index >= 0 && index < days.size() ? AXIS_DAY.format(days.get(index)) : "";
            }));
        }
        trendY.setLowerBound(0);
        trendY.setUpperBound(DashboardFigures.countAxisTop(peak));
        trendY.setTickUnit(DashboardFigures.countAxisStep(peak));

        trendSeries.getData().setAll(points);
        for (int i = 0; i < points.size(); i++) {
            Node marker = points.get(i).getNode();
            // A marker on every empty hour drew a chain of rings along the axis;
            // the line already shows the zeros, so only hours with batches get one.
            if (trend.get(i).batches() == 0) {
                marker.setVisible(false);
                continue;
            }
            Tooltip tooltip = new Tooltip(tooltips.get(i));
            tooltip.setShowDelay(Duration.millis(100));
            Tooltip.install(marker, tooltip);
        }
    }

    private void showRecent(DashboardPeriod period, List<RecentBatch> batches) {
        recentRows.getChildren().setAll(recentRow("recent-header",
                heading("Batch No"), heading("Recipe"), heading("Vehicle"), heading("Quantity"),
                heading("Time"), heading("Status")));

        if (batches.isEmpty()) {
            Label empty = new Label("No batches " + period.within() + ".");
            empty.getStyleClass().add("state-message");
            VBox emptyBox = new VBox(empty);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(28, 0, 28, 0));
            recentRows.getChildren().add(emptyBox);
            return;
        }

        DateTimeFormatter timeFormat = period.singleDay() ? TIME : DAY_AND_TIME;
        for (RecentBatch batch : batches) {
            recentRows.getChildren().add(recentRow("recent-row",
                    cell(batch.batchNumber()),
                    cell(batch.recipeName()),
                    cell(batch.vehicleNumber()),
                    cell(VOLUME.format(batch.producedM3()) + " m³"),
                    cell(timeFormat.format(batch.cycleDateTime())),
                    statusPill(batch.status())));
        }
    }

    private static GridPane recentRow(String styleClass, Label... cells) {
        GridPane row = new GridPane();
        row.getStyleClass().add(styleClass);
        for (int i = 0; i < cells.length; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(RECENT_COLUMNS[i]);
            if (i == QUANTITY_COLUMN) {
                // Figures line up on the decimal point when they share a right edge.
                column.setHalignment(HPos.RIGHT);
                cells[i].getStyleClass().add("recent-number");
            }
            row.getColumnConstraints().add(column);
            row.add(cells[i], i, 0);
        }
        return row;
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("recent-heading");
        return label;
    }

    private static Label cell(String text) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().add("recent-cell");
        return label;
    }

    /** Coloured, but always with the status in words beside it. */
    private static Label statusPill(BatchStatus status) {
        if (status == null) {
            return cell("Unknown");
        }
        String text = switch (status) {
            case PENDING -> "Pending";
            case IN_PROGRESS -> "In Progress";
            case PAUSED -> "Paused";
            case STOPPED -> "Stopped";
            case COMPLETED -> "Completed";
        };
        Label pill = new Label(text);
        pill.getStyleClass().addAll("batch-status", "batch-status-" + status.name().toLowerCase().replace('_', '-'));
        return pill;
    }

    private static StringConverter<Number> axisLabels(Function<Number, String> format) {
        return new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return format.apply(value);
            }

            @Override
            public Number fromString(String text) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** An outline icon, scaled inside a Group so layout makes room for its drawn size. */
    private static Group icon(String path, String styleClass, double scale) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFillRule(FillRule.EVEN_ODD);
        svg.getStyleClass().add(styleClass);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return new Group(svg);
    }

    /** One of the coloured headline figures across the top. */
    private static final class KpiTile extends StackPane {

        private final Label value = new Label("-");
        private final Label unit = new Label();
        private final Label change = new Label(" ");

        KpiTile(String title, String iconPath, String colourClass, String explanation) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("kpi-tile-title");
            value.getStyleClass().add("kpi-tile-value");
            // Its own smaller label: as part of the 28px bold figure, the ³ in m³
            // drew nearly as tall as the digits.
            unit.getStyleClass().add("kpi-tile-unit");
            change.getStyleClass().add("kpi-tile-change");
            change.setWrapText(true);
            HBox figure = new HBox(4, value, unit);
            figure.setAlignment(Pos.BASELINE_LEFT);

            Group icon = icon(iconPath, "kpi-tile-icon", 1.6);
            StackPane.setAlignment(icon, Pos.TOP_RIGHT);
            getChildren().addAll(icon, new VBox(4, titleLabel, figure, change));

            getStyleClass().addAll("kpi-tile", colourClass);
            // Equal preferred widths, so the row shares its space out evenly.
            setPrefWidth(200);
            setMinWidth(170);
            setMaxWidth(Double.MAX_VALUE);
            Tooltip.install(this, new Tooltip(explanation));
        }

        void show(String valueText, String unitText, String changeText) {
            value.setText(valueText);
            unit.setText(unitText);
            change.setText(changeText);
        }
    }

    /** A coloured dot and its words - the colour is never the only sign. */
    private static final class StatusItem extends HBox {

        static final String UP = "status-dot-up";
        static final String DOWN = "status-dot-down";
        static final String IDLE = "status-dot-idle";

        private final Circle dot = new Circle(4);
        private final Label text = new Label();

        StatusItem() {
            super(6);
            text.getStyleClass().add("status-item");
            getChildren().addAll(dot, text);
            setAlignment(Pos.CENTER_LEFT);
        }

        void show(String name, String state, String dotClass) {
            text.setText(name + " " + state);
            dot.getStyleClass().setAll(dotClass);
        }
    }
}
