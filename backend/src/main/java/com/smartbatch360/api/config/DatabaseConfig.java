package com.smartbatch360.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Backend MySQL connection settings, entered once via the first-run setup
 * dialog (DatabaseSetupDialog) and persisted to an external properties file
 * so re-installs/updates never need it re-entered.
 *
 * The database name is fixed - SmartBatch360 always uses its own
 * "smartbatch360" schema, created automatically if it does not exist yet
 * (see DatabaseBootstrap). The user only ever supplies a Database URL,
 * Username and Password, in that order.
 */
public final class DatabaseConfig {

    public static final String DATABASE_NAME = "smartbatch360";

    private final String host;
    private final String port;
    private final String username;
    private final String password;

    private DatabaseConfig(String host, String port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Parses whatever the user typed into the "Database URL" field: a bare
     * host ("localhost"), host:port ("localhost:3306"), or a pasted
     * jdbc:mysql://host:port/schema?options URL - any schema/query suffix is
     * ignored since the database name is fixed.
     */
    public static DatabaseConfig of(String dbUrlInput, String username, String password) {
        String s = dbUrlInput.trim().replaceFirst("(?i)^jdbc:mysql://", "");

        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        int questionMark = s.indexOf('?');
        if (questionMark >= 0) {
            s = s.substring(0, questionMark);
        }

        String host;
        String port = "3306";
        int colon = s.lastIndexOf(':');
        if (colon >= 0) {
            host = s.substring(0, colon);
            port = s.substring(colon + 1);
        } else {
            host = s;
        }

        return new DatabaseConfig(host, port, username.trim(), password);
    }

    public static Optional<DatabaseConfig> loadFrom(Path path) {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            return Optional.empty();
        }
        String host = props.getProperty("db.host");
        String port = props.getProperty("db.port", "3306");
        String username = props.getProperty("db.username");
        String password = props.getProperty("db.password");
        if (host == null || host.isBlank() || username == null || password == null) {
            return Optional.empty();
        }
        return Optional.of(new DatabaseConfig(host, port, username, password));
    }

    public void saveTo(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.username", username);
        props.setProperty("db.password", password);
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, "SmartBatch360 database connection. Edit or delete this file to reconfigure.");
        }
    }

    public String host() {
        return host;
    }

    public String port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String database() {
        return DATABASE_NAME;
    }

    /** Server-level connection (no schema in the URL) - used only to create the database if it's missing. */
    public String adminJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
}
