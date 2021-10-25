package org.cryptomator.hub.config;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class HubConfigSourceFactory implements ConfigSourceFactory {

    private static final String DEFAULT_CONFIG_LOCATION = "~/hub.properties";

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        var s = configSourceContext.getValue("hub.config.path");
        final String hubConfigLocation;
        if (s != null) {
            hubConfigLocation = s.getValue();
        } else {
            hubConfigLocation = DEFAULT_CONFIG_LOCATION;
        }
        var hubConfig = new HubConfigSource(resolveHome(hubConfigLocation));
        return List.of(hubConfig);
    }

    private static Path resolveHome(String p) {
        if (p.startsWith("~")) {
            return Path.of(System.getProperty("user.home"), p.substring(1));
        } else {
            return Path.of(p);
        }
    }
}
