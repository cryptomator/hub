package org.cryptomator.hub.config;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import java.util.List;

@ApplicationScoped
public class HubConfigSourceFactory implements ConfigSourceFactory {

    private static final String USER_HOME = System.getProperty("user.home");

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        var s = configSourceContext.getValue("hub.config.path");
        final String hubConfigLocation;
        if (s != null) {
            hubConfigLocation = s.getValue();
        } else {
            hubConfigLocation = USER_HOME;
        }
        var hubConfig = new HubConfigSource(resolveHome(hubConfigLocation));
        return List.of(hubConfig);
    }

    private static Path resolveHome(String p) {
        if (p.startsWith("~")) {
            return Path.of(USER_HOME, p.substring(1));
        } else {
            System.out.println(p);
            return Path.of(p);
        }
    }
}
