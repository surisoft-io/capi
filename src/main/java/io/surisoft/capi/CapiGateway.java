package io.surisoft.capi;

import io.surisoft.capi.configuration.ExternalConfigurationLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CapiGateway {
    public static void main(String[] args) {
        SpringApplication capi = new SpringApplication(CapiGateway.class);
        capi.addListeners(new ExternalConfigurationLoader());
        capi.run(args);
    }
}