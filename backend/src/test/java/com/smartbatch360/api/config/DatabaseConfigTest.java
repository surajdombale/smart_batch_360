package com.smartbatch360.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigTest {

    @Test
    void parsesBareHost() {
        DatabaseConfig config = DatabaseConfig.of("localhost", "root", "secret");

        assertThat(config.host()).isEqualTo("localhost");
        assertThat(config.port()).isEqualTo("3306");
    }

    @Test
    void parsesHostAndPort() {
        DatabaseConfig config = DatabaseConfig.of("db.example.com:3307", "root", "secret");

        assertThat(config.host()).isEqualTo("db.example.com");
        assertThat(config.port()).isEqualTo("3307");
    }

    @Test
    void stripsJdbcPrefixSchemaAndQueryString() {
        DatabaseConfig config = DatabaseConfig.of(
                "jdbc:mysql://192.168.1.10:3306/ignoredSchema?useSSL=false", "root", "secret");

        assertThat(config.host()).isEqualTo("192.168.1.10");
        assertThat(config.port()).isEqualTo("3306");
    }

    @Test
    void databaseNameIsAlwaysFixed() {
        DatabaseConfig config = DatabaseConfig.of("localhost:3306/someOtherName", "root", "secret");

        assertThat(config.database()).isEqualTo("smartbatch360");
    }

    @Test
    void adminUrlHasNoSchema() {
        DatabaseConfig config = DatabaseConfig.of("localhost:3306", "root", "secret");

        assertThat(config.adminJdbcUrl()).isEqualTo(
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    @Test
    void loadFromMissingFileReturnsEmpty(@TempDir Path tempDir) {
        Optional<DatabaseConfig> loaded = DatabaseConfig.loadFrom(tempDir.resolve("does-not-exist.properties"));

        assertThat(loaded).isEmpty();
    }

    @Test
    void savedConfigRoundTripsThroughLoad(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("config").resolve("smartbatch360.properties");
        DatabaseConfig original = DatabaseConfig.of("localhost:3306", "smartbatch360", "smartbatch360");

        original.saveTo(path);
        Optional<DatabaseConfig> loaded = DatabaseConfig.loadFrom(path);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().host()).isEqualTo("localhost");
        assertThat(loaded.get().port()).isEqualTo("3306");
        assertThat(loaded.get().username()).isEqualTo("smartbatch360");
        assertThat(loaded.get().password()).isEqualTo("smartbatch360");
    }
}
