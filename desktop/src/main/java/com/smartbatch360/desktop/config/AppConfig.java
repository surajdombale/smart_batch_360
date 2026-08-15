package com.smartbatch360.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads desktop configuration (currently just the backend API base URL) from
 * application.properties. Override order (highest priority first):
 * -Dapi.base-url system property, API_BASE_URL environment variable, then the
 * bundled default - so a packaged .exe can point at a different backend
 * without a rebuild (env var is the more reliable override for a native
 * launcher, where JVM system-property passthrough isn't guaranteed).
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

        String configured = properties.getProperty("api.base-url", "http://localhost:8081");
        String envOverride = System.getenv("API_BASE_URL");
        String withEnvFallback = (envOverride != null && !envOverride.isBlank()) ? envOverride : configured;
        this.apiBaseUrl = System.getProperty("api.base-url", withEnvFallback);
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }
}
