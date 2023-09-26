package org.cryptomator.hub;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import java.sql.SQLException;

@Liveness
@ApplicationScoped
public class HubHealthCheck implements HealthCheck {

	@Inject
	AgroalDataSource dataSource;

	@Override
	public HealthCheckResponse call() {
		try (var connection = dataSource.getConnection()) {
			try (var statement = connection.createStatement()) {
				statement.execute("""
						SELECT 1 FROM "settings";
						""");
				return HealthCheckResponse.up("Hub database is ready");
			}
		} catch (SQLException e) {
			return HealthCheckResponse.down("Hub database is unhealthy");
		}
	}

}
