package org.cryptomator.hub.config;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class HubConfigSourceFactory implements ConfigSourceFactory {

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        var s = configSourceContext.getValue("hub.config.path");
        if (s != null) {
            var hubConfig = new HubConfigSource(resolveHome(s.getValue()));
            return List.of(hubConfig);
        } else {
            return List.of();
        }
    }

    private static Path resolveHome(String p) {
        if (p.startsWith("~")) {
            return Path.of(System.getProperty("user.home"), p.substring(1));
        } else {
            System.out.println(p);
            return Path.of(p);
        }
    }
}
