package org.cryptomator.hub.config;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
            throw new UncheckedIOException("Unable to persist Hub config to " + configPath, e);
        }
    }


    synchronized Map<String, String> load() {
        var prop = new Properties();
        try (var in = Files.newInputStream(configPath, StandardOpenOption.READ)) {
            prop.load(in);
        } catch (NoSuchFileException e) {
            LOG.debugf("No hub config file found at %s.", configPath);
        } catch (IOException e) {
            LOG.warn(String.format("Unable to read hub config from %s, creating empty.", configPath), e);
        }
        return (Map<String, String>) (Map<?, ?>) prop;
    }


}
