package com.smartbatch360.desktop.common;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

/**
 * Reusable list-screen shell for the master-data CRUD pattern
 * (docs/02_UI_REFERENCE.md "UI implementation rule"): page header, toolbar
 * (search/Refresh/Add), table, and loading/empty/error/data state switching,
 * plus a success/error notification banner.
 *
 * Search (requested by the user, 2026-08-23, "in all tables from now on") is
 * a client-side substring filter over whatever is currently loaded - Phase 1
 * datasets are small and there is no backend search/pagination endpoint, so
 * this stays simple and instant rather than adding API surface for it.
 */
public class CrudListView<T> {

    private final BorderPane root = new BorderPane();
    private final Toolbar toolbar;
    private final NotificationBanner banner = new NotificationBanner();
    private final TableView<T> table = new TableView<>();
    private final StackPane centerStack = new StackPane();

    private final ObservableList<T> sourceItems = FXCollections.observableArrayList();
    private final FilteredList<T> filteredItems = new FilteredList<>(sourceItems);
    private final Function<T, String> searchTextExtractor;

    private final VBox loadingView;
    private final VBox emptyView;
    private final VBox errorView;
    private final Label errorMessageLabel = new Label();

    private Runnable onRetry = () -> {
    };

    /**
     * @param searchTextExtractor concatenates whichever fields of a row should be
     *                            searchable (e.g. name + phone + status); matching is a
     *                            case-insensitive substring check against this string.
     */
    public CrudListView(String title, String subtitle, String addLabel, Function<T, String> searchTextExtractor) {
        this.searchTextExtractor = searchTextExtractor;

        PageHeader pageHeader = new PageHeader(title, subtitle);
        toolbar = new Toolbar(addLabel);
        toolbar.getSearchField().textProperty().addListener((obs, old, text) -> applyFilter(text));

        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(filteredItems);
        table.setPlaceholder(new Label("No matching records. Try a different search."));

        loadingView = stateView(new ProgressIndicator(), "Loading...", null, null);
        emptyView = stateView(null, "No records found", "Use \"" + addLabel + "\" to create the first one.", null);

        Button retryButton = new Button("Retry");
        retryButton.getStyleClass().add("button-secondary");
        retryButton.setOnAction(e -> onRetry.run());
        errorView = stateView(null, "Something went wrong", null, retryButton);
        errorView.getStyleClass().add("state-error");
        VBox.setVgrow(errorMessageLabel, javafx.scene.layout.Priority.NEVER);
        errorView.getChildren().add(1, errorMessageLabel);

        loadingView.setAlignment(Pos.CENTER);
        emptyView.setAlignment(Pos.CENTER);
        errorView.setAlignment(Pos.CENTER);

        VBox top = new VBox(pageHeader, banner, toolbar);
        root.setTop(top);
        root.setCenter(centerStack);
        root.getStyleClass().add("content-area");

        showLoading();
    }

    private VBox stateView(Region icon, String title, String message, Button action) {
        VBox box = new VBox();
        box.getStyleClass().add("state-container");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("state-title");
        box.getChildren().add(titleLabel);
        if (icon != null) {
            box.getChildren().add(0, icon);
        }
        if (message != null) {
            Label messageLabel = new Label(message);
            messageLabel.getStyleClass().add("state-message");
            messageLabel.setWrapText(true);
            box.getChildren().add(messageLabel);
        }
        if (action != null) {
            box.getChildren().add(action);
        }
        return box;
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        filteredItems.setPredicate(item -> q.isEmpty()
                || searchTextExtractor.apply(item).toLowerCase().contains(q));
    }

    public Region getView() {
        return root;
    }

    public TableView<T> getTable() {
        return table;
    }

    public Button getAddButton() {
        return toolbar.getAddButton();
    }

    public Button getRefreshButton() {
        return toolbar.getRefreshButton();
    }

    public NotificationBanner getBanner() {
        return banner;
    }

    public void setOnRetry(Runnable onRetry) {
        this.onRetry = onRetry;
    }

    public void showLoading() {
        centerStack.getChildren().setAll(loadingView);
    }

    public void showError(String message) {
        errorMessageLabel.setText(message);
        centerStack.getChildren().setAll(errorView);
    }

    public void showData(List<T> items) {
        sourceItems.setAll(items);
        centerStack.getChildren().setAll(items.isEmpty() ? emptyView : table);
    }
}
