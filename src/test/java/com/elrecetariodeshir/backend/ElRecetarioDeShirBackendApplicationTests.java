package com.elrecetariodeshir.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
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

		Integer historyTableCount = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name = 'flyway_schema_history'
				""",
				Integer.class);

		Integer markerTableCount = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				  AND table_name = 'database_foundation_marker'
				""",
				Integer.class);

		Integer successfulMigrationCount = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '1'
				  AND success = 1
				""",
				Integer.class);

		assertThat(historyTableCount).isEqualTo(1);
		assertThat(markerTableCount).isEqualTo(1);
		assertThat(successfulMigrationCount).isEqualTo(1);

		MigrateResult secondMigrationAttempt = flyway.migrate();

		assertThat(secondMigrationAttempt.migrationsExecuted).isZero();
	}

}
