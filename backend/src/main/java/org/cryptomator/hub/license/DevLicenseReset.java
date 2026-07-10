package org.cryptomator.hub.license;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.cryptomator.hub.entities.Settings;
import org.jboss.logging.Logger;

// TODO TEMPORARY, remove before merging: clears the stored license on every start in dev mode, so the license setup flow can be re-experienced by simply pressing "s" in quarkus:dev
@ApplicationScoped
@IfBuildProfile("dev")
public class DevLicenseReset {

	private static final Logger LOG = Logger.getLogger(DevLicenseReset.class);

	@Transactional
	void resetLicense(@Observes StartupEvent event, Settings.Repository settingsRepo) {
		var settings = settingsRepo.get();
		if (settings.getLicenseKey() != null) {
			settings.setLicenseKey(null);
			settingsRepo.persistAndFlush(settings);
		}
		LOG.warn("TEMPORARY dev mechanism: cleared stored license, starting in setup mode.");
	}

}
