package io.surisoft.capi.lb.controller;

import io.surisoft.capi.lb.zipkin.CapiZipkinTracer;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.zipkin.ZipkinTracer;
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
      locations = "classpath:test-capi-configuration-application.properties"
)
class TestCapiConfiguration {

    @Autowired
    CapiZipkinTracer zipkinTracer;

    @Autowired
    HttpComponent httpComponent;

    @Test
    void testZipkin() {
        Assertions.assertNotNull(zipkinTracer);
    }

    @Test
    void testHttpComponent() {
        Assertions.assertNotNull(httpComponent);
    }
}