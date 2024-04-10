package org.cryptomator.hub;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.cryptomator.hub.rollback.DBRollbackAfter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;

@QuarkusTest
public class RollbackTestIT {
	@Inject
	public Flyway flyway;

	@Inject
	AgroalDataSource dataSource;

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class WithFlywayCleanup {

		@Test
		@Order(1)
		@DBRollbackAfter
		public void changeDB() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE "settings"
						SET "hub_id" = '42', "license_key" = 'yodel'
						WHERE "id" = 0;
						""");
			}
		}

		@Test
		@Order(2)
		public void testDB() throws SQLException {
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
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class NoCleanup {

		@Test
		@Order(1)
		public void changeDB() throws SQLException {
			try (var c = dataSource.getConnection(); var s = c.createStatement()) {
				s.execute("""
						UPDATE "settings"
						SET "hub_id" = '42', "license_key" = 'yodel'
						WHERE "id" = 0;
						""");
			}
		}

		@Test
		@Order(2)
		public void testDB() throws SQLException {
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
