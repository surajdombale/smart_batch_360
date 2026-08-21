package com.smartbatch360.api.config;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Runs before Spring Boot starts (see SmartBatch360ApiApplication.main). On
 * a normal desktop install, prompts once for the MySQL connection via
 * DatabaseSetupDialog, auto-creates the "smartbatch360" database if it
 * doesn't exist yet - no manual mysql script needed - and persists the
 * connection to an external config file so it is asked again only if the
 * stored credentials stop working.
 *
 * Headless environments (tests, CI, `mvn verify`/`mvn test`) skip this
 * entirely and fall back to the pre-existing DB_HOST/DB_USERNAME/... env-var
 * (or application.yml default) behaviour - this class is never reached by
 * the test suite, since @DataJpaTest/@WebMvcTest never call main().
 */
public final class DatabaseBootstrap {

    private static final Path CONFIG_PATH =
            Path.of(System.getProperty("user.dir"), "config", "smartbatch360.properties");

    private DatabaseBootstrap() {
    }

    public static void ensureConfigured() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        DatabaseConfig config = DatabaseConfig.loadFrom(CONFIG_PATH).orElse(null);
        String error = null;

        while (true) {
            if (config == null) {
                Optional<DatabaseConfig> entered = DatabaseSetupDialog.prompt(error);
                if (entered.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "SmartBatch360 Server cannot start without a database connection.",
                            "Setup cancelled", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                    return;
                }
                config = entered.get();
            }

            try {
                ensureDatabaseExists(config);
                config.saveTo(CONFIG_PATH);
                break;
            } catch (Exception e) {
                error = "Could not connect or create the database:\n" + e.getMessage();
                config = null;
            }
        }

        applyToSystemProperties(config);
    }

    private static void ensureDatabaseExists(DatabaseConfig config) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                config.adminJdbcUrl(), config.username(), config.password());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS `" + config.database() + "` CHARACTER SET utf8mb4");
        }
    }

    /** application.yml already reads ${DB_HOST:...}/${DB_PORT:...}/etc. - these satisfy those placeholders. */
    private static void applyToSystemProperties(DatabaseConfig config) {
        System.setProperty("DB_HOST", config.host());
        System.setProperty("DB_PORT", config.port());
        System.setProperty("DB_USERNAME", config.username());
        System.setProperty("DB_PASSWORD", config.password());
        System.setProperty("DB_NAME", config.database());
    }
}
