package com.smartbatch360.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads desktop configuration (currently just the backend API base URL) from
 * application.properties, with a -Dapi.base-url system property override so
 * the same build can point at different environments without a code change.
 */
public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private final String apiBaseUrl;

    private AppConfig() {
        Properties properties = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }

        String configured = properties.getProperty("api.base-url", "http://localhost:8080");
        this.apiBaseUrl = System.getProperty("api.base-url", configured);
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }
}
