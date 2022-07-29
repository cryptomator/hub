package org.cryptomator.hub.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.UUID;

public class V1_0_1__Initialize_Settings extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		try (PreparedStatement statement = context.getConnection().prepareStatement("INSERT INTO \"billing\" VALUES (?, ?, ?)")) {
			statement.setInt(1, 0);
			statement.setString(2, UUID.randomUUID().toString());
			statement.setNull(3, Types.VARCHAR);
			statement.executeUpdate();
		}
	}

}
