package org.cryptomator.hub.config;

import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.stream.StreamSupport;

public class HubConfigProducer {

	@Produces
	@ApplicationScoped
	public static HubConfig getInstance() {
		return (HubConfig) StreamSupport.stream(ConfigProvider.getConfig().getConfigSources().spliterator(), false)
				.filter(s -> s instanceof HubConfig)
				.findFirst()
				.get();
	}

}
