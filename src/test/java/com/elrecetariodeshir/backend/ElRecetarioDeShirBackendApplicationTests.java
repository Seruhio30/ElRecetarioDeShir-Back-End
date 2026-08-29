package com.elrecetariodeshir.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "app.media.storage.root=${java.io.tmpdir}/elrecetariodeshir-test-media")
class ElRecetarioDeShirBackendApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Flyway flyway;

	@Test
	void contextLoadsAndDatabaseFoundationIsReady() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.isValid(2)).isTrue();
			assertThat(connection.getCatalog()).isEqualTo("elrecetariodeshir_db");
		}

		Integer historyTableCount = tableCount("flyway_schema_history");
		Integer markerTableCount = tableCount("database_foundation_marker");

		assertThat(historyTableCount).isEqualTo(1);
		assertThat(markerTableCount).isEqualTo(1);

		assertSuccessfulMigration("1");
		assertSuccessfulMigration("2");

		List<String> recipeTables = jdbcTemplate.queryForList(
				"""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name IN (
				      'recipe',
				      'recipe_ingredient',
				      'recipe_step',
				      'recipe_image'
				  )
				ORDER BY table_name
				""",
				String.class);

		assertThat(recipeTables).containsExactly(
				"recipe",
				"recipe_image",
				"recipe_ingredient",
				"recipe_step");

		MigrateResult secondMigrationAttempt = flyway.migrate();

		assertThat(secondMigrationAttempt.migrationsExecuted).isZero();
	}

	private Integer tableCount(String tableName) {
		return jdbcTemplate.queryForObject(
				"""
					SELECT COUNT(*)
					FROM information_schema.tables
					WHERE table_schema = DATABASE()
					  AND table_name = ?
					""",
				Integer.class,
				tableName);
	}

	private void assertSuccessfulMigration(String version) {
		Integer successfulMigrationCount = jdbcTemplate.queryForObject(
				"""
					SELECT COUNT(*)
					FROM flyway_schema_history
					WHERE version = ?
					  AND success = 1
					""",
				Integer.class,
				version);

		assertThat(successfulMigrationCount).isEqualTo(1);
	}
}
