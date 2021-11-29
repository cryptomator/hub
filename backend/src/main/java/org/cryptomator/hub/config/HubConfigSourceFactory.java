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
		// dev console can't "see" application.properties. In this case, there is no need for a HubConfig
		if (configPath.getValue() == null) {
			return List.of();
		} else {
			var hubConfig = new HubConfig(configPath.getValue());
			return List.of(hubConfig);
		}
	}

}
