package com.smartbatch360.api;

import com.smartbatch360.api.config.DatabaseBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartBatch360ApiApplication {

    public static void main(String[] args) {
        // Must run before the Spring context (and its DataSource) is created:
        // on a fresh install this is what prompts for DB credentials and
        // creates the database itself. No-op in headless environments
        // (tests/CI) - see DatabaseBootstrap.
        DatabaseBootstrap.ensureConfigured();
        SpringApplication.run(SmartBatch360ApiApplication.class, args);
    }
}
