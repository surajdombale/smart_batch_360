package com.smartbatch360.desktop.settings;

import com.smartbatch360.api.config.DatabaseConfig;
import com.smartbatch360.desktop.common.NotificationBanner;
import com.smartbatch360.desktop.plant.PlantSettingsApiClient;
import com.smartbatch360.desktop.plant.PlantSettingsRequestDto;
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

    private final PlantSettingsApiClient plantApiClient = new PlantSettingsApiClient();
    private final TextField supervisorField = new TextField();
    private final TextField mixerCapacityField = new TextField();
    private final TextField plantCapacityField = new TextField();
    private final Button savePlantButton = new Button("Save Plant Details");
    private final Label plantStatusLabel = new Label();

    public SettingsView() {
        root.getStyleClass().add("content-area");
        root.setTop(new PageHeader("Settings", "Configure SmartBatch360."));

        Tab dbTab = new Tab("Database Connection", buildDatabaseConnectionTab());
        dbTab.setClosable(false);

        Tab plantTab = new Tab("Plant Details", buildPlantDetailsTab());
        plantTab.setClosable(false);

        TabPane tabPane = new TabPane(dbTab, plantTab);
        root.setCenter(tabPane);

        loadSavedConnection();
        refreshStatus();
        loadPlantDetails();
    }


    /**
     * Settings > Plant Details: the plant's own figures, moved here from Company
     * Details on 07-Oct-2026. The mixer capacity is the one Production divides a
     * load by, so a change here takes effect on the next load.
     */
    private VBox buildPlantDetailsTab() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);

        Label heading = new Label("Plant Details");
        heading.getStyleClass().add("card-title");

        supervisorField.setPromptText("e.g. R. Patil");
        mixerCapacityField.setPromptText("0.1 to 10, e.g. 1");
        plantCapacityField.setPromptText("hourly output, e.g. 30");

        plantStatusLabel.getStyleClass().add("state-message");
        plantStatusLabel.setWrapText(true);

        savePlantButton.getStyleClass().add("button-primary");
        savePlantButton.setOnAction(e -> savePlantDetails());

        card.getChildren().addAll(heading,
                field("Supervisor Name", supervisorField),
                field("Mixer Capacity (m3)", mixerCapacityField),
                field("Plant Capacity (m3 per hour)", plantCapacityField),
                savePlantButton, plantStatusLabel);

        VBox wrapper = new VBox(card);
        wrapper.setPadding(new Insets(16));
        return wrapper;
    }

    private VBox field(String label, TextField control) {
        Label caption = new Label(label);
        caption.getStyleClass().add("form-label");
        return new VBox(4, caption, control);
    }

    private void loadPlantDetails() {
        plantApiClient.get().whenComplete((settings, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                plantStatusLabel.setText("Plant details could not be loaded.");
                return;
            }
            supervisorField.setText(settings.supervisorName());
            mixerCapacityField.setText(plain(settings.mixerCapacityM3()));
            plantCapacityField.setText(plain(settings.plantCapacityM3PerHour()));
        }));
    }

    private void savePlantDetails() {
        java.math.BigDecimal mixer = parseDecimalOrNull(mixerCapacityField.getText());
        java.math.BigDecimal plant = parseDecimalOrNull(plantCapacityField.getText());
        if (mixer == null && !mixerCapacityField.getText().isBlank()) {
            plantStatusLabel.setText("Mixer capacity must be a number, for example 1 or 1.5.");
            return;
        }
        if (plant == null && !plantCapacityField.getText().isBlank()) {
            plantStatusLabel.setText("Plant capacity must be a number, for example 30.");
            return;
        }

        savePlantButton.setDisable(true);
        plantStatusLabel.setText("Saving...");
        plantApiClient.save(new PlantSettingsRequestDto(supervisorField.getText(), mixer, plant))
                .whenComplete((saved, throwable) -> Platform.runLater(() -> {
                    savePlantButton.setDisable(false);
                    if (throwable != null) {
                        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                        plantStatusLabel.setText(cause.getMessage() != null
                                ? cause.getMessage() : "Plant details could not be saved.");
                    } else {
                        plantStatusLabel.setText("Saved. Production uses these from the next load.");
                    }
                }));
    }

    private String plain(java.math.BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private java.math.BigDecimal parseDecimalOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
