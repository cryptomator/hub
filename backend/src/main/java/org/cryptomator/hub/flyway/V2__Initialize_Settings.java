package org.cryptomator.hub.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

@Deprecated
public class V2__Initialize_Settings extends BaseJavaMigration {

	@Override
	public void migrate(Context context) {
		// no-op: Replaced by V3__Initialize_Settings.sql
	}

}
