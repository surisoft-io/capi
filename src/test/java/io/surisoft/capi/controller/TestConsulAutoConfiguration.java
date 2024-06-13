package io.surisoft.capi.controller;

import io.surisoft.capi.service.ConsistencyChecker;
import io.surisoft.capi.service.ConsulNodeDiscovery;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestConsulAutoConfiguration {

    @Autowired
    ConsulNodeDiscovery consulNodeDiscovery;

    @Autowired
    ConsistencyChecker consistencyChecker;

    @Test
    void testConsulNodeDiscovery() {
        Assertions.assertNotNull(consulNodeDiscovery);
    }

    @Test
    void testConsistencyChecker() {
        Assertions.assertNotNull(consistencyChecker);
    }

}