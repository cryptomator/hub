package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;
import java.util.UUID;

public class V1_0_1__Initialize_Billing extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		try (Statement statement = context.getConnection().createStatement()) {
			statement.executeUpdate("INSERT INTO billing VALUES (0, '" + UUID.randomUUID() + "', NULL)");
		}
	}

}
