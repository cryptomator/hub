package org.cryptomator.hub;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.atomic.AtomicReference;

@QuarkusTest
public class RollbackTest {
	@Inject
	Flyway flyway;

	@Inject
	AgroalDataSource dataSource;

	@Nested
	class WithFlywayCleanup {

		@BeforeEach
		public void rollback() {
			flyway.clean();
			flyway.migrate();
		}

		@Test
		public void test1() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
					UPDATE "settings"
					SET "hub_id" = '42', "license_key" = 'yodel'
					WHERE "id" = 0;
					""");
			}
		}

		@Test
		public void test2() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				var result = s.executeQuery("""
					SELECT *
					FROM "settings"
					WHERE "id" = 0;
					""");
				result.next();
				Assertions.assertNotEquals("yodel", result.getString("license_key"));
			}
		}
	}

	@Nested
	class NoCleanup {

		@Test
		public void test1() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
					UPDATE "settings"
					SET "hub_id" = '42', "license_key" = 'yodel'
					WHERE "id" = 0;
					""");
			}
		}

		@Test
		public void test2() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				var result = s.executeQuery("""
					SELECT *
					FROM "settings"
					WHERE "id" = 0;
					""");
				result.next();
				Assertions.assertEquals("yodel", result.getString("license_key"));
			}
		}
	}
}
