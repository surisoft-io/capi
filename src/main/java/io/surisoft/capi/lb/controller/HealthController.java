package io.surisoft.capi.lb.controller;

import io.surisoft.capi.lb.service.ConsulNodeDiscovery;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class HealthController implements HealthIndicator {
    @Override
    public Health health() {
        if(ConsulNodeDiscovery.isConnectedToConsul()) {
            return Health.up().build();
        }
        return Health.down().withDetail("reason", "Consul not available").build();
    }
}