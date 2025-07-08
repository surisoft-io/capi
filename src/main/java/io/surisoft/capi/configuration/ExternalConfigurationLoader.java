package io.surisoft.capi.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.*;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

public class ExternalConfigurationLoader implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ExternalConfigurationLoader.class);
    private static final String CONFIG_FILE_PATH = "capi-configuration";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();

        String externalPath = System.getProperty(CONFIG_FILE_PATH);

        if (externalPath == null || externalPath.isBlank()) {
            log.warn("CAPI environment variable {} not set, skipping external file configuration.", CONFIG_FILE_PATH);
            return;
        }

        Resource resource = new FileSystemResource(externalPath);
        if (!resource.exists()) {
            log.warn("CAPI configuration file not found at: {}", externalPath);
            return;
        }

        try {
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            PropertySource<?> yamlProps = loader.load(
                    "externalConfig", resource
            ).get(0);

            env.getPropertySources().addFirst(yamlProps);
            log.info("Loaded CAPI configuration file from: {}", externalPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file from " + externalPath, e);
        }
    }
}