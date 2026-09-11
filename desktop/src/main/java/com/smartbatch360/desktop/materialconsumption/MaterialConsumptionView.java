package com.smartbatch360.desktop.materialconsumption;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.common.PageHeader;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Material Consumption (docs/02_UI_REFERENCE.md's "Material Consumption
 * reference"): target vs achieved vs variance/wastage, grouped by day/week/
 * month, built from existing Batch/BatchMaterial data. The table came first
 * (2026-08-26); the chart above it followed on 2026-09-11.
 *
 * The chart answers one question - which materials came in over or under
 * their target - so it is an emphasis chart rather than a colourful one:
 * Achieved is the series that matters and takes the accent blue, Target is
 * context and sits in gray beside it. It sums each material across the
 * filtered range; the per-period breakdown stays in the table underneath,
 * which is also the chart's accessible twin. Both follow the one filter row.
 */
public class MaterialConsumptionView {

    private final MaterialConsumptionApiClient apiClient = new MaterialConsumptionApiClient();

    private final BorderPane root = new BorderPane();
    private final NotificationBanner banner = new NotificationBanner();
    private final StackPane centerStack = new StackPane();
    private final TableView<MaterialConsumptionDto> table = new TableView<>();

    /** Mark spec: bars never thicker than this, however few materials there are. */
    private static final double MAX_BAR_THICKNESS = 24;
    /** The surface-coloured gap between a material's Target and Achieved bars. */
    private static final double BAR_GAP = 2;

    private final CategoryAxis materialAxis = new CategoryAxis();
    private final NumberAxis quantityAxis = new NumberAxis();
    private final BarChart<String, Number> chart = new BarChart<>(materialAxis, quantityAxis);
    private final StackPane chartBody = new StackPane();
    private final Label chartEmpty = new Label("No batches match these filters.");
    private VBox chartCard;

    private final TextField materialNameField = new TextField();
    private final DatePicker dateFromField = new DatePicker();
    private final DatePicker dateToField = new DatePicker();
    private final ComboBox<MaterialConsumptionGroupBy> groupByField =
            new ComboBox<>(FXCollections.observableArrayList(MaterialConsumptionGroupBy.values()));

    public MaterialConsumptionView() {
        PageHeader header = new PageHeader("Material Consumption",
                "Target vs achieved material usage and variance, by day, week or month.");

        VBox top = new VBox(10, header, banner, buildFilterPanel());
        root.setTop(top);
        root.setCenter(centerStack);
        root.getStyleClass().add("content-area");

        setupTable();
        setupChart();
        chartCard = buildChartCard();
        search();
    }

    private VBox buildFilterPanel() {
        materialNameField.setPromptText("e.g. Cement");
        groupByField.getSelectionModel().select(MaterialConsumptionGroupBy.DAY);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0,
                labeled("Material", materialNameField),
                labeled("Date From", dateFromField),
                labeled("Date To", dateToField),
                labeled("Group By", groupByField));

        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("button-primary");
        searchButton.setOnAction(e -> search());

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

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No material consumption for these filters."));

        TableColumn<MaterialConsumptionDto, String> periodCol = new TableColumn<>("Period");
        periodCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().period()));

        TableColumn<MaterialConsumptionDto, String> materialCol = new TableColumn<>("Material");
        materialCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().materialName()));

        TableColumn<MaterialConsumptionDto, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(cd -> new SimpleStringProperty(
                formatQuantity(cd.getValue().totalTarget(), cd.getValue().unit())));

        TableColumn<MaterialConsumptionDto, String> achievedCol = new TableColumn<>("Achieved");
        achievedCol.setCellValueFactory(cd -> new SimpleStringProperty(
                formatQuantity(cd.getValue().totalAchieved(), cd.getValue().unit())));

        TableColumn<MaterialConsumptionDto, MaterialConsumptionDto> varianceCol = new TableColumn<>("Variance");
        varianceCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue()));
        varianceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(MaterialConsumptionDto row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().removeAll("status-up", "status-down");
                if (empty || row == null) {
                    setText(null);
                    return;
                }
                BigDecimal variance = row.variance();
                setText(formatSignedQuantity(variance, row.unit()));
                if (variance.compareTo(BigDecimal.ZERO) > 0) {
                    getStyleClass().add("status-down"); // over-consumption/wastage
                } else if (variance.compareTo(BigDecimal.ZERO) < 0) {
                    getStyleClass().add("status-up"); // under-consumption/savings
                }
            }
        });

        TableColumn<MaterialConsumptionDto, String> batchesCol = new TableColumn<>("Batches");
        batchesCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().batchCount())));

        table.getColumns().setAll(List.of(periodCol, materialCol, targetCol, achievedCol, varianceCol, batchesCol));
    }

    private void setupChart() {
        quantityAxis.setLabel("kg");
        quantityAxis.setForceZeroInRange(true);
        quantityAxis.setMinorTickVisible(false);
        materialAxis.setTickMarkVisible(false);

        chart.getStyleClass().add("consumption-chart");
        chart.setAnimated(false);
        // Our own legend in the card header instead. JavaFX drops a chart's
        // built-in legend without warning when it judges the plot too short,
        // which left two series told apart by colour alone.
        chart.setLegendVisible(false);
        chart.setBarGap(BAR_GAP);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeRowFillVisible(false);
        // Short on purpose: at the default window size a taller chart left the
        // period table underneath only three rows, and the table is where the
        // actual figures live.
        chart.setPrefHeight(200);
        chart.setMinHeight(170);

        // A BarChart sizes its bars from whatever room each category gets, so
        // with two or three materials they balloon into slabs. Spend the
        // spare room on the gap between categories instead.
        materialAxis.widthProperty().addListener((o, was, is) -> capBarThickness());

        chartEmpty.getStyleClass().add("state-message");
        chartBody.getChildren().setAll(chart);
    }

    private VBox buildChartCard() {
        Label title = new Label("Target vs achieved, by material");
        title.getStyleClass().add("state-title");
        Label subtitle = new Label("Summed across the dates and material filtered above. "
                + "Hover a bar for its figures; the table below breaks them down by period.");
        subtitle.getStyleClass().add("state-message");
        subtitle.setWrapText(true);

        HBox legend = new HBox(16, legendKey("Target", "legend-swatch-target"),
                legendKey("Achieved", "legend-swatch-achieved"));
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(6, 0, 0, 0));

        VBox card = new VBox(4, title, subtitle, legend, chartBody);
        card.getStyleClass().add("card");
        return card;
    }

    /** Swatch beside the name; the name itself stays in text colour. */
    private HBox legendKey(String name, String swatchStyle) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("legend-swatch", swatchStyle);
        swatch.setMinSize(10, 10);
        swatch.setMaxSize(10, 10);
        Label label = new Label(name);
        label.getStyleClass().add("legend-label");
        HBox key = new HBox(6, swatch, label);
        key.setAlignment(Pos.CENTER_LEFT);
        return key;
    }

    private void capBarThickness() {
        int categories = materialAxis.getCategories().size();
        int series = chart.getData().size();
        if (categories == 0 || series == 0) {
            return;
        }
        double perCategory = materialAxis.getWidth() / categories;
        double barsAtCap = series * MAX_BAR_THICKNESS + (series - 1) * BAR_GAP;
        chart.setCategoryGap(Math.max(8, perCategory - barsAtCap));
    }

    /** One entry per material, summed over every period the filters returned. */
    private record MaterialTotal(String material, String unit, BigDecimal target, BigDecimal achieved) {
        BigDecimal variance() {
            return achieved.subtract(target);
        }
    }

    private List<MaterialTotal> totalsByMaterial(List<MaterialConsumptionDto> rows) {
        Map<String, MaterialTotal> totals = new LinkedHashMap<>();
        for (MaterialConsumptionDto row : rows) {
            BigDecimal target = row.totalTarget() != null ? row.totalTarget() : BigDecimal.ZERO;
            BigDecimal achieved = row.totalAchieved() != null ? row.totalAchieved() : BigDecimal.ZERO;
            totals.merge(row.materialName(),
                    new MaterialTotal(row.materialName(), row.unit(), target, achieved),
                    (a, b) -> new MaterialTotal(a.material(), a.unit(),
                            a.target().add(b.target()), a.achieved().add(b.achieved())));
        }
        // Nominal categories have no order of their own; largest first makes
        // the comparison easiest to read.
        List<MaterialTotal> sorted = new ArrayList<>(totals.values());
        sorted.sort(Comparator.comparing(MaterialTotal::target).reversed());
        return sorted;
    }

    private void showChart(List<MaterialConsumptionDto> rows) {
        List<MaterialTotal> totals = totalsByMaterial(rows);
        if (totals.isEmpty()) {
            chartBody.getChildren().setAll(chartEmpty);
            return;
        }

        // Series order fixes colour: 0 = Target (gray, context), 1 = Achieved
        // (blue, the point). Keep it - the stylesheet keys on the index.
        XYChart.Series<String, Number> target = new XYChart.Series<>();
        target.setName("Target");
        XYChart.Series<String, Number> achieved = new XYChart.Series<>();
        achieved.setName("Achieved");

        for (MaterialTotal total : totals) {
            XYChart.Data<String, Number> t = new XYChart.Data<>(total.material(), total.target().doubleValue());
            XYChart.Data<String, Number> a = new XYChart.Data<>(total.material(), total.achieved().doubleValue());
            attachTooltip(t, total);
            attachTooltip(a, total);
            target.getData().add(t);
            achieved.getData().add(a);
        }

        materialAxis.getCategories().setAll(totals.stream().map(MaterialTotal::material).toList());
        chart.getData().setAll(List.of(target, achieved));
        chartBody.getChildren().setAll(chart);
        Platform.runLater(this::capBarThickness);
    }

    /** Both bars of a material show the same figures: the comparison is the point. */
    private void attachTooltip(XYChart.Data<String, Number> data, MaterialTotal total) {
        String text = total.material()
                + "\nAchieved  " + formatQuantity(total.achieved(), total.unit())
                + "\nTarget  " + formatQuantity(total.target(), total.unit())
                + "\nVariance  " + formatSignedQuantity(total.variance(), total.unit());
        data.nodeProperty().addListener((o, was, node) -> {
            if (node != null) {
                Tooltip tooltip = new Tooltip(text);
                tooltip.setShowDelay(Duration.millis(120));
                Tooltip.install(node, tooltip);
            }
        });
    }

    private String formatQuantity(BigDecimal value, String unit) {
        return value == null ? "" : value.toPlainString() + (unit != null ? " " + unit : "");
    }

    private String formatSignedQuantity(BigDecimal value, String unit) {
        if (value == null) {
            return "";
        }
        String sign = value.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return sign + value.toPlainString() + (unit != null ? " " + unit : "");
    }

    private void resetFilters() {
        materialNameField.clear();
        dateFromField.setValue(null);
        dateToField.setValue(null);
        groupByField.getSelectionModel().select(MaterialConsumptionGroupBy.DAY);
        search();
    }

    private void search() {
        centerStack.getChildren().setAll(loadingIndicator());

        String materialName = materialNameField.getText() == null || materialNameField.getText().isBlank()
                ? null : materialNameField.getText().trim();

        apiClient.search(materialName, dateFromField.getValue(), dateToField.getValue(),
                        groupByField.getValue())
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        showError(throwable);
                    } else {
                        showResults(result);
                    }
                }));
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
        Label title = new Label("Unable to load material consumption");
        title.getStyleClass().add("state-title");
        Label msg = new Label(message);
        msg.getStyleClass().add("state-message");
        msg.setWrapText(true);
        Button retry = new Button("Retry");
        retry.getStyleClass().add("button-secondary");
        retry.setOnAction(e -> search());
        box.getChildren().addAll(title, msg, retry);
        centerStack.getChildren().setAll(box);
    }

    private void showResults(List<MaterialConsumptionDto> result) {
        table.setItems(FXCollections.observableArrayList(result));
        showChart(result);

        VBox content = new VBox(12, chartCard, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        centerStack.getChildren().setAll(content);
    }

    public Region getView() {
        return root;
    }
}
