package io.surisoft.capi.lb.controller;

import io.surisoft.capi.lb.processor.ConsulNodeDiscovery;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-consul-configuration-application.properties"
)
class TestConsulAutoConfiguration {

    @Autowired
    ConsulNodeDiscovery consulNodeDiscovery;

    @Autowired
    RouteBuilder timerRouteBuilder;

    @Test
    void testConsulNodeDiscovery() {
        Assertions.assertNotNull(consulNodeDiscovery);
    }

    @Test
    void testTimerRouteBuilder() {
        Assertions.assertNotNull(timerRouteBuilder);
    }
}