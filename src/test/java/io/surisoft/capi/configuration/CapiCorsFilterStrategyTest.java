package io.surisoft.capi.configuration;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CapiCorsFilterStrategyTest {

    private CapiCorsFilterStrategy capiCorsFilterStrategyUnderTest;

    public static final String[] CAPI_ACCESS_CONTROL_ALLOW_HEADERS = {
            "Origin",
            "Accept",
            "X-Requested-With",
            "Content-Type",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "x-referrer",
            "Authorization",
            "Authorization-Propagation",
            "X-Csrf-Request",
            "Cache-Control",
            "pragma",
            "gem-context",
            "x-syncmode",
            "X-Total-Count",
            "Last-Event-ID",
            "X-B3-Sampled",
            "X-B3-SpanId",
            "X-B3-TraceId",
            "X-B3-ParentSpanId",
            "X-Auth-Url-Index",
            "X-Apigateway-Impersonated-Cookie-Name",
            "Vary"
    };

    @BeforeEach
    void setUp() {
        capiCorsFilterStrategyUnderTest = new CapiCorsFilterStrategy(Arrays.stream(CAPI_ACCESS_CONTROL_ALLOW_HEADERS).toList());
    }

    @Test
    void testInitialize() {
        capiCorsFilterStrategyUnderTest.initialize();
        Assertions.assertTrue(capiCorsFilterStrategyUnderTest.getOutFilter().contains("Access-Control-Allow-Origin"));
        Assertions.assertTrue(capiCorsFilterStrategyUnderTest.getOutFilter().contains("Access-Control-Allow-Credentials"));
    }

    @Test
    void testApplyFilterToExternalHeaders() {
        // Setup
        CamelContext context = new DefaultCamelContext();
        final Exchange exchange = new DefaultExchange(context);


        // Run the test
        final boolean result1 = capiCorsFilterStrategyUnderTest.applyFilterToExternalHeaders("Access-Control-Allow-Origin", "true", exchange);
        final boolean result2 = capiCorsFilterStrategyUnderTest.applyFilterToExternalHeaders("SomeNonFilteredHeader", "true", exchange);
        // Verify the results
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }
}
