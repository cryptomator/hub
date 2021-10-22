package org.cryptomator.hub.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//TODO: reevaluate if CDI is possible
// because configs are so early created, this might not be possible
public class HubConfigSource implements ConfigSource {

    private static final String HUB_CONFIGSOURCE_NAME = "HubConfig";

    private HubConfigWriter writer;
    private final ConcurrentHashMap<String, String> config;

    public HubConfigSource(Path p) {
        this.config = new ConcurrentHashMap();
        writer = new HubConfigWriter(p);
        config.putAll(writer.read());
    }

    @Override
    public synchronized Map<String, String> getProperties() {
        return config;
    }

    @Override
    public Set<String> getPropertyNames() {
        return config.keySet();
    }

    @Override
    public int getOrdinal() {
        return 900; //TODO
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
        if (oldVal != null && !oldVal.equals(value)) {
            writer.persist(config);
        }
    }

}
