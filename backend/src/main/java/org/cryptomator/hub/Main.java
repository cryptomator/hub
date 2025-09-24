package org.cryptomator.hub;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.cryptomator.hub.license.LicenseHolder;
import org.jboss.logging.Logger;

@QuarkusMain
public class Main implements QuarkusApplication {

	private static final Logger LOG = Logger.getLogger(Main.class);

	@Inject
	LicenseHolder license;

	@Override
	public int run(String... args) throws Exception {
		try {
			license.ensureLicenseExists();
		} catch (RuntimeException e) {
			LOG.error("Failed to validate license, shutting down...", e);
			return 1;
		}
		Quarkus.waitForExit();
		return 0;
	}

}
