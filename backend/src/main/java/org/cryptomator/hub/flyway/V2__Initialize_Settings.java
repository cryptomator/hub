package org.cryptomator.hub.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @deprecated This used to generated the hub id. It got replaced by <code>V3__Initialize_Settings.sql</code>, though. Despite being dead code,
 * this class must remain present in order for Flyway to work correctly on existing installations. May be removed when we start with a new <a href="https://flywaydb.org/documentation/concepts/baselinemigrations">baseline migration</a>
 * <p>
 * Note: Quarkus seems to fail to detect this file during hot reloading (see issue 212).
 *
 * @see <a href="https://github.com/cryptomator/hub/issues/183">Issue 183</a>
 * @see <a href="https://github.com/cryptomator/hub/pull/184">PR 184</a>
 * @see <a href="https://github.com/cryptomator/hub/issues/212">Issue 212</a>
 */
@Deprecated
public class V2__Initialize_Settings extends BaseJavaMigration {

	@Override
	public void migrate(Context context) {
		// no-op: Replaced by V3__Initialize_Settings.sql
	}

}
