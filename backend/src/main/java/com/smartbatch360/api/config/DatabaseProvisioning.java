package com.smartbatch360.api.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the "smartbatch360" database if it doesn't exist yet, over a raw
 * admin JDBC connection (no schema in the URL). Shared by both the
 * console-based DatabaseBootstrap (standalone backend deployments) and the
 * desktop app's in-process Settings > Database Connection screen - the
 * SQL/JDBC logic is identical either way, only who triggers it differs.
 */
public final class DatabaseProvisioning {

    private DatabaseProvisioning() {
    }

    public static void ensureDatabaseExists(DatabaseConfig config) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                config.adminJdbcUrl(), config.username(), config.password());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS `" + config.database() + "` CHARACTER SET utf8mb4");
        }
    }
}
