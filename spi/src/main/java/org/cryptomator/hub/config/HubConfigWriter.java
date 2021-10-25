package org.cryptomator.hub.config;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

class HubConfigWriter {

	private static final Logger LOG = Logger.getLogger(HubConfigWriter.class);
	private static final String HUB_CONFIG_DESCRIPTION = "This file contains configuration values for Crypotmator Hub.";

	private final Path configPath;
	private final ExecutorService executorService;


	private AtomicReference<Properties> nextJob;

	public HubConfigWriter(Path configPath) {
		this.configPath = configPath;
		this.executorService = Executors.newSingleThreadExecutor();
		this.nextJob = new AtomicReference<>(null);
	}

	synchronized void persist(Map<String, String> config) {
		var prop = new Properties();
		prop.putAll(config);
		try (var out = Files.newOutputStream(configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			prop.store(out, HUB_CONFIG_DESCRIPTION);
		} catch (IOException e) {
			LOG.warn("Unable to persist hub config to {}.", configPath, e);
		}
	}


	Map<String, String> read() {
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
