package com.smartbatch360.desktop.settings;

import com.smartbatch360.api.config.DatabaseConfig;
import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.common.PageHeader;
import com.smartbatch360.desktop.server.EmbeddedServer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Settings screen. Phase 1 scope is intentionally limited to a single
 * "Database Connection" tab - the other settings described in the source
 * documents (Plant, PLC communication, backup/restore, user management,
 * general preferences) remain out of scope (docs/06_SCOPE_AND_ROADMAP.md).
 * The TabPane structure leaves room to add those later without reshaping
 * this screen.
 */
public class SettingsView {

    private final BorderPane root = new BorderPane();
    private final NotificationBanner banner = new NotificationBanner();

    private final TextField urlField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label();
    private final Button connectButton = new Button("Connect & Save");
    private final ProgressIndicator progress = new ProgressIndicator();

    public SettingsView() {
        root.getStyleClass().add("content-area");
        root.setTop(new PageHeader("Settings", "Configure SmartBatch360."));

        Tab dbTab = new Tab("Database Connection", buildDatabaseConnectionTab());
        dbTab.setClosable(false);

        TabPane tabPane = new TabPane(dbTab);
        root.setCenter(tabPane);

        loadSavedConnection();
        refreshStatus();
    }

    private VBox buildDatabaseConnectionTab() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(420);
        card.setPadding(new Insets(20));

        Label heading = new Label("MySQL Connection");
        heading.getStyleClass().add("page-header-subtitle");

        Label urlLabel = new Label("Database URL (host:port)");
        urlLabel.getStyleClass().add("form-label");
        urlField.setPromptText("localhost:3306");

        Label userLabel = new Label("Username");
        userLabel.getStyleClass().add("form-label");

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("form-label");

        Label note = new Label("Use a MySQL account with permission to create databases (an "
                + "admin/root account works). The \"smartbatch360\" database and its tables "
                + "are created automatically - nothing to run by hand.");
        note.getStyleClass().add("state-message");
        note.setWrapText(true);

        progress.setMaxSize(18, 18);
        progress.setVisible(false);
        progress.setManaged(false);

        connectButton.getStyleClass().add("button-primary");
        connectButton.setOnAction(e -> connectAndSave());

        card.getChildren().addAll(
                heading, statusLabel,
                urlLabel, urlField,
                userLabel, usernameField,
                passLabel, passwordField,
                note, banner,
                new javafx.scene.layout.HBox(10, connectButton, progress)
        );

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(16, 0, 0, 0));
        return wrapper;
    }

    private void loadSavedConnection() {
        EmbeddedServer.savedConfig().ifPresent(config -> {
            urlField.setText(config.host() + ":" + config.port());
            usernameField.setText(config.username());
            passwordField.setText(config.password());
        });
    }

    private void refreshStatus() {
        boolean running = EmbeddedServer.isRunning();
        statusLabel.setText(running ? "Status: Connected" : "Status: Not connected");
        statusLabel.getStyleClass().removeAll("status-up", "status-down");
        statusLabel.getStyleClass().add(running ? "status-up" : "status-down");
    }

    private void connectAndSave() {
        banner.hide();

        if (urlField.getText().isBlank() || usernameField.getText().isBlank()) {
            banner.showError("Database URL and Username are required.");
            return;
        }

        boolean wasRunning = EmbeddedServer.isRunning();
        DatabaseConfig config = DatabaseConfig.of(urlField.getText(), usernameField.getText(), passwordField.getText());

        setBusy(true);
        Thread.ofVirtual().name("db-connect").start(() -> {
            String error = null;
            try {
                EmbeddedServer.start(config);
            } catch (Exception e) {
                error = describeError(e);
            }
            String finalError = error;
            Platform.runLater(() -> {
                setBusy(false);
                refreshStatus();
                if (finalError != null) {
                    banner.showError(finalError);
                } else if (wasRunning) {
                    banner.showSuccess("Connection saved. Restart SmartBatch360 for the new "
                            + "database connection to take effect.");
                } else {
                    banner.showSuccess("Connected. The database and tables were created "
                            + "automatically - the app is ready to use.");
                }
            });
        });
    }

    private void setBusy(boolean busy) {
        connectButton.setDisable(busy);
        progress.setVisible(busy);
        progress.setManaged(busy);
    }

    private String describeError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return "Could not connect or create the database:\n" + message;
    }

    public javafx.scene.layout.Region getView() {
        return root;
    }
}
