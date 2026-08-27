package com.smartbatch360.desktop.server;

import com.smartbatch360.api.SmartBatch360ApiApplication;
import com.smartbatch360.api.config.DatabaseConfig;
import com.smartbatch360.api.config.DatabaseProvisioning;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Runs the Spring Boot backend (REST controllers, JPA, Flyway - unchanged
 * from the standalone server) in the same JVM as the JavaFX UI, so the whole
 * app ships and installs as one process/one .exe.
 *
 * The saved connection lives in the user's profile (%APPDATA%\SmartBatch360
 * on Windows, ~/.smartbatch360 elsewhere) rather than next to the installed
 * app, so it survives reinstalls/updates and never runs into install-folder
 * write permissions.
 *
 * If no connection is saved yet, startIfConfigured() is a no-op: the UI
 * still launches normally, screens show their existing connection-error/
 * retry states, and Settings > Database Connection is where the user sets
 * one up (which calls start() directly).
 */
public final class EmbeddedServer {

    private static volatile ConfigurableApplicationContext context;

    /**
     * True once a startup attempt has finished, successfully or not.
     *
     * Deliberately a plain volatile rather than something derived from
     * {@link #isRunning()}: start() is synchronized and holds the class lock
     * for the whole Spring boot sequence (~10-20s), so isRunning() blocks for
     * that entire time. Callers that need to know "is the backend still coming
     * up?" without stalling - ApiClient's startup retry - read this instead.
     */
    private static volatile boolean startupSettled;

    private EmbeddedServer() {
    }

    public static Path configPath() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null
                ? Path.of(appData, "SmartBatch360")
                : Path.of(System.getProperty("user.home"), ".smartbatch360");
        return base.resolve("smartbatch360.properties");
    }

    public static Optional<DatabaseConfig> savedConfig() {
        return DatabaseConfig.loadFrom(configPath());
    }

    public static synchronized boolean isRunning() {
        return context != null && context.isRunning();
    }

    /** Called once at app startup. Silently does nothing if no connection has been saved yet. */
    public static void startIfConfigured() {
        try {
            savedConfig().ifPresent(config -> {
                try {
                    start(config);
                } catch (Exception e) {
                    // Leave the server stopped - the UI's existing error/retry states handle this,
                    // and Settings > Database Connection lets the user fix or re-enter it.
                    System.err.println("Could not start with the saved database connection: " + e.getMessage());
                }
            });
        } finally {
            startupSettled = true;
        }
    }

    /**
     * Whether the initial startup attempt has finished (either way). Never
     * blocks - see the field's note. Used to decide whether a failed API call
     * is "the backend hasn't finished booting" or a real failure worth
     * reporting straight away.
     */
    public static boolean isStartupSettled() {
        return startupSettled;
    }

    /**
     * Creates the database if needed, starts (or restarts) the embedded backend, and persists
     * the connection. Throws on failure - the caller (SettingsView) is responsible for surfacing
     * that to the user; nothing is saved or left running on failure.
     */
    public static synchronized void start(DatabaseConfig config) throws SQLException, IOException {
        DatabaseProvisioning.ensureDatabaseExists(config);

        System.setProperty("DB_HOST", config.host());
        System.setProperty("DB_PORT", config.port());
        System.setProperty("DB_USERNAME", config.username());
        System.setProperty("DB_PASSWORD", config.password());
        System.setProperty("DB_NAME", config.database());

        if (context != null) {
            context.close();
            context = null;
        }
        context = SpringApplication.run(SmartBatch360ApiApplication.class);
        startupSettled = true;

        config.saveTo(configPath());
    }

    public static synchronized void stop() {
        if (context != null) {
            context.close();
            context = null;
        }
    }
}
