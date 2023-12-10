package io.surisoft.capi.configuration;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapiCorsFilterStrategyTest {

    private CapiCorsFilterStrategy capiCorsFilterStrategyUnderTest;

    @BeforeEach
    void setUp() {
        capiCorsFilterStrategyUnderTest = new CapiCorsFilterStrategy();
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
