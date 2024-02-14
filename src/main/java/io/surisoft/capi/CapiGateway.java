package io.surisoft.capi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class CapiGateway {
    public static void main(String[] args) {
        SpringApplication.run(CapiGateway.class, args);
    }
}
