package com.elrecetariodeshir.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

class TestDatabaseSafetyInitializerTests {

    @Test
    void allowsConfiguredTestDatabase() {
        try (GenericApplicationContext context = contextWithUrl(
                "jdbc:mysql://192.168.16.1:3306/elrecetariodeshir_test")) {

            assertThatCode(() ->
                    new TestDatabaseSafetyInitializer().initialize(context))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsConfiguredDevelopmentDatabaseBeforeContextInitialization() {
        try (GenericApplicationContext context = contextWithUrl(
                "jdbc:mysql://192.168.16.1:3306/elrecetariodeshir_db")) {

            assertThatThrownBy(() ->
                    new TestDatabaseSafetyInitializer().initialize(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Refusing to initialize tests")
                    .hasMessageContaining("elrecetariodeshir_db");
        }
    }

    @Test
    void extractsExactTestDatabaseNameFromJdbcUrl() {
        assertThat(TestDatabaseSafetyInitializer.extractDatabaseName(
                "jdbc:mysql://192.168.16.1:3306/elrecetariodeshir_test"))
                .isEqualTo("elrecetariodeshir_test");
    }

    @Test
    void extractsDatabaseNameWhenJdbcUrlHasParameters() {
        assertThat(TestDatabaseSafetyInitializer.extractDatabaseName(
                "jdbc:mysql://192.168.16.1:3306/elrecetariodeshir_test?useSSL=false&serverTimezone=UTC"))
                .isEqualTo("elrecetariodeshir_test");
    }

    @Test
    void identifiesDevelopmentDatabaseExactly() {
        String databaseName = TestDatabaseSafetyInitializer.extractDatabaseName(
                "jdbc:mysql://192.168.16.1:3306/elrecetariodeshir_db");

        assertThat(databaseName)
                .isEqualTo(TestDatabaseSafetyInitializer.DEVELOPMENT_DATABASE);
    }

    @Test
    void rejectsNonMysqlJdbcUrl() {
        assertThatThrownBy(() ->
                TestDatabaseSafetyInitializer.extractDatabaseName(
                        "jdbc:postgresql://localhost/elrecetariodeshir_test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MySQL JDBC URL");
    }

    @Test
    void rejectsJdbcUrlWithoutDatabaseName() {
        assertThatThrownBy(() ->
                TestDatabaseSafetyInitializer.extractDatabaseName(
                        "jdbc:mysql://192.168.16.1:3306/"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database name");
    }

    private GenericApplicationContext contextWithUrl(String jdbcUrl) {
        GenericApplicationContext context = new GenericApplicationContext();

        TestPropertyValues.of(
                "spring.datasource.url=" + jdbcUrl)
                .applyTo(context);

        return context;
    }
}
