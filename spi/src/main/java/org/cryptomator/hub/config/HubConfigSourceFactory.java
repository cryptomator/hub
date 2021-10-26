package org.cryptomator.hub.config;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class HubConfigSourceFactory implements ConfigSourceFactory {

    private static final String HUBCONFIG_PROPERTY_KEY = "hub.config.path";

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        var s = configSourceContext.getValue(HUBCONFIG_PROPERTY_KEY);
        if (s.getValue() == null) {
            throw new IllegalStateException("Property \"" + HUBCONFIG_PROPERTY_KEY + "\" to point to hub config file is missing");
        }
        var hubConfigPersistence = new HubConfigPersistence(Path.of(s.getValue()));
        var hubConfig = new HubConfigSource(hubConfigPersistence);
        return List.of(hubConfig);
    }

}
