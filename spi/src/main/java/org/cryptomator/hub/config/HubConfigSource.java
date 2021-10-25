package org.cryptomator.hub.config;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

public class HubConfigSource implements ConfigSource {

    private static final String HUB_CONFIGSOURCE_NAME = "HubConfig";

    private final HubConfigPersistence persistence;
    private final ConcurrentHashMap<String, String> config;

    public HubConfigSource(HubConfigPersistence persistence) {
        this.config = new ConcurrentHashMap();
        this.persistence = persistence;

        config.putAll(persistence.read());
    }

    @Override
    public Map<String, String> getProperties() {
        return config;
    }

    @Override
    public Set<String> getPropertyNames() {
        return config.keySet();
    }

    @Override
    //for default sources and their precedence see https://quarkus.io/guides/config-reference#configuration-sources
    public int getOrdinal() {
        return 240;
    }

    @Override
    public String getValue(String s) {
        return config.get(s);
    }

    @Override
    public String getName() {
        return HUB_CONFIGSOURCE_NAME;
    }

    public void setProperty(String key, String value) {
        String oldVal = config.put(key, value);
        if (oldVal == null || (oldVal != null && !oldVal.equals(value))) {
            persistence.persist(config);
        }
    }

    public static HubConfigSource getInstance() {
        return (HubConfigSource) StreamSupport.stream(ConfigProvider.getConfig().getConfigSources().spliterator(), false)
                .filter(s -> s instanceof HubConfigSource)
                .findFirst()
                .get();
    }

}
