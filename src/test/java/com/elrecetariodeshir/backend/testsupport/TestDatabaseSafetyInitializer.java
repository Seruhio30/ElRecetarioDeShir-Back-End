package com.elrecetariodeshir.backend.testsupport;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

public final class TestDatabaseSafetyInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String DEVELOPMENT_DATABASE = "elrecetariodeshir_db";
    static final String TEST_DATABASE = "elrecetariodeshir_test";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        String jdbcUrl = environment.getRequiredProperty("spring.datasource.url");
        String databaseName = extractDatabaseName(jdbcUrl);

        if (DEVELOPMENT_DATABASE.equals(databaseName)) {
            throw new IllegalStateException(
                    "Refusing to initialize tests against development database: "
                            + DEVELOPMENT_DATABASE);
        }

        if (!TEST_DATABASE.equals(databaseName)) {
            throw new IllegalStateException(
                    "Integration tests require database "
                            + TEST_DATABASE
                            + " but configured database is: "
                            + databaseName);
        }
    }

    static String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("Test datasource URL must be configured");
        }

        if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException(
                    "Integration tests require a MySQL JDBC URL");
        }

        try {
            URI uri = new URI(jdbcUrl.substring("jdbc:".length()));
            String path = uri.getPath();

            if (path == null || path.length() <= 1) {
                throw new IllegalStateException(
                        "Test datasource URL must include a database name");
            }

            String databaseName = path.substring(1);

            if (databaseName.isBlank() || databaseName.contains("/")) {
                throw new IllegalStateException(
                        "Test datasource URL contains an invalid database name");
            }

            return databaseName;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "Test datasource URL is invalid",
                    exception);
        }
    }
}
