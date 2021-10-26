package org.cryptomator.hub.config;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class HubConfigPersistence {

	private static final Logger LOG = Logger.getLogger(HubConfigPersistence.class);
	private static final String HUB_CONFIG_DESCRIPTION = "Cryptomator Hub configuration persistence file";

	private final Path configPath;

	HubConfigPersistence(Path configPath) {
		this.configPath = configPath;
	}

	synchronized void persist(Map<String, String> config) {
		var prop = new Properties();
		prop.putAll(config);
		try (var out = Files.newOutputStream(configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			prop.store(out, HUB_CONFIG_DESCRIPTION);
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to persist Hub config to "+configPath,e);
		}
	}


	Map<String, String> load() {
		Map<String, String> config = new HashMap<>();
		try (var in = Files.newInputStream(configPath, StandardOpenOption.READ)) {
			var prop = new Properties();
			prop.load(in);
			prop.forEach((k, v) -> config.put((String) k, (String) v));
		} catch (IOException e) {
			LOG.info("Unable to read hub config from {}, creating empty.", configPath, e);
		}
		return config;
	}


}
