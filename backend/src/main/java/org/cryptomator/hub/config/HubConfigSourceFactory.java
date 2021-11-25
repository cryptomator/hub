package org.cryptomator.hub.config;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.List;

public class HubConfigSourceFactory implements ConfigSourceFactory {

	private static final String HUBCONFIG_PROPERTY_KEY = "hub.config.path";

	@Override
	public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
		var configPath = configSourceContext.getValue(HUBCONFIG_PROPERTY_KEY);
		var hubConfig = new HubConfig(configPath.getValue());
		return List.of(hubConfig);
	}
}
