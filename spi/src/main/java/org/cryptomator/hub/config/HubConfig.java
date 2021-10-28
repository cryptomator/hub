package org.cryptomator.hub.config;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.stream.StreamSupport;

public class HubConfig implements ConfigSource {

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

	public synchronized void setProperty(Key key, String value) {
		String oldVal = (String) config.setProperty(key.toString(), value);
		if (oldVal == null || !oldVal.equals(value)) {
			persistence.persist(config);
		}
	}

	@Produces
	@Named("HubConfig")
	public static HubConfig getInstance() {
		return (HubConfig) StreamSupport.stream(ConfigProvider.getConfig().getConfigSources().spliterator(), false)
				.filter(s -> s instanceof HubConfig)
				.findFirst()
				.get();
	}

	public enum Key {

		//TODO: replace by real property key
		DUMMY_VALUE("dummy.value");

		private final String actualKeyString;

		Key(String actualKeyString) {
			this.actualKeyString = actualKeyString;
		}

		@Override
		public String toString() {
			return actualKeyString;
		}
	}

}
