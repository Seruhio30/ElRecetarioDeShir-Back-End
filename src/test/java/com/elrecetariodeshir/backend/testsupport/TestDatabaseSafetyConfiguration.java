package com.elrecetariodeshir.backend.testsupport;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseSafetyConfiguration {

    @Bean
    TestDatabaseCatalogGuard testDatabaseCatalogGuard(DataSource dataSource) {
        return new TestDatabaseCatalogGuard(dataSource);
    }

    static final class TestDatabaseCatalogGuard {

        TestDatabaseCatalogGuard(DataSource dataSource) {
            verify(dataSource);
        }

        private void verify(DataSource dataSource) {
            try (Connection connection = dataSource.getConnection()) {
                String catalog = connection.getCatalog();

                if (TestDatabaseSafetyInitializer.DEVELOPMENT_DATABASE.equals(catalog)) {
                    throw new IllegalStateException(
                            "Refusing to run integration tests against development database: "
                                    + TestDatabaseSafetyInitializer.DEVELOPMENT_DATABASE);
                }

                if (!TestDatabaseSafetyInitializer.TEST_DATABASE.equals(catalog)) {
                    throw new IllegalStateException(
                            "Integration tests require database "
                                    + TestDatabaseSafetyInitializer.TEST_DATABASE
                                    + " but connected to: "
                                    + catalog);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException(
                        "Unable to verify integration test database",
                        exception);
            }
        }
    }
}
