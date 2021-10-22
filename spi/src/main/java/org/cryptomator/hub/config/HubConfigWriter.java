package org.cryptomator.hub.config;

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

public class HubConfigWriter {

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
        nextJob.set(prop);
        executorService.submit(this::write);
    }

    private Runnable write() {
        return () -> {
            Properties prop;
            synchronized (this) {
                prop = nextJob.getAndSet(null);
            }
            if (prop != null) {
                try (var out = Files.newOutputStream(configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    prop.store(out, "TODO");
                } catch (IOException e) {
                    //TODO: LOG
                    e.printStackTrace();
                }
            }
        };
    }

    Map<String, String> read() {
        Map<String, String> config = new HashMap<>();
        try (var in = Files.newInputStream(configPath, StandardOpenOption.READ)) {
            var prop = new Properties();
            prop.load(in);
            prop.forEach((k, v) -> config.put((String) k, (String) v));
        } catch (IOException e) {
            //TODO: Log
            e.printStackTrace();
        }
        return config;
    }


}
