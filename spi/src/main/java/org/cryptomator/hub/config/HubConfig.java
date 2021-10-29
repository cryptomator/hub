package org.cryptomator.hub.config;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import javax.enterprise.inject.Vetoed;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

@Vetoed
public class HubConfig implements ConfigSource {

	public static final String SETUP_COMPLETED = "hub.setupCompleted";
	public static final String OIDC_ENDPOINT = "quarkus.oidc.auth-server-url";

	private static final Logger LOG = Logger.getLogger(HubConfig.class);
	private static final String HUB_CONFIGSOURCE_NAME = "HubConfig";
	private static final String HUB_CONFIGPATH_PROPERTY_KEY = "hub.config.path";

	private final HubConfigPersistence persistence;
	private final Properties config;

	public HubConfig() {
		var configPath = System.getProperty(HUB_CONFIGPATH_PROPERTY_KEY);
		if (configPath == null) {
			throw new IllegalStateException("Property " + HUB_CONFIGPATH_PROPERTY_KEY + " not set.");
		}

		LOG.info("Hub config persists to " + configPath);
		this.config = new Properties();
		this.persistence = new HubConfigPersistence(Path.of(configPath));

		persistence.load(config);
	}

	@Override
	public Set<String> getPropertyNames() {
		return config.stringPropertyNames();
	}

	@Override
	// for default sources and their precedence see https://quarkus.io/guides/config-reference#configuration-sources
	public int getOrdinal() {
		return 270;
	}

	@Override
	public String getValue(String key) {
		return config.getProperty(key);
	}

	@Override
	public String getName() {
		return HUB_CONFIGSOURCE_NAME;
	}

	private synchronized void setProperty(String key, String value) {
		String oldVal = (String) config.setProperty(key, value);
		if (oldVal == null || !oldVal.equals(value)) {
			persistence.persist(config);
		}
	}

	public boolean isSetupCompleted() {
		var val = getValue(SETUP_COMPLETED);
		return Boolean.parseBoolean(val);
	}

	public void setSetupCompleted(boolean completed) {
		setProperty(SETUP_COMPLETED, Boolean.toString(completed));
	}

	public void setOidcAuthEndpoint(String oidcAuthEndpoint) {
		setProperty(OIDC_ENDPOINT, oidcAuthEndpoint);
	}

	public String getOidcAuthEndpoint() {
		return getValue(OIDC_ENDPOINT);
	}
}
