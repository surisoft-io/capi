package io.surisoft.capi.lb.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.surisoft.capi.lb.cache.ConsulCacheManager;
import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.ConsulNodeDiscovery;
import io.surisoft.capi.lb.processor.MetricsProcessor;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-application.properties"
)
class TestConsulNodeDiscovery {

    private static final String SERVICES_RESPONSE = """
            {
                "consul": [],
                "dummy": [
                    "group=dev",
                    "no-root-context"
                ]
            }""";
    private static final String SERVICE_DUMMY_RESPONSE = """
            [
              {
                "ID": "d5b14430-d939-a0a9-90fa-a7e4f7dac84b",
                "Node": "client-1",
                "ServiceKind": "",
                "ServiceID": "dev-1",
                "ServiceName": "dummy",
                "ServiceTags": [
                  "capi",
                  "service-call",
                  "group=dev",
                  "x-forwarded-prefix",
                  "X-B3-TraceId",
                  "no-root-context"
                ],
                "ServiceAddress": "localhost",
                "ServiceWeights": {
                  "Passing": 1,
                  "Warning": 1
                },
                "ServiceMeta": {},
                "ServicePort": 8081,
                "ServiceSocketPath": "",
                "ServiceEnableTagOverride": false
              }
            ]""";

    @Autowired
    ApiUtils apiUtils;

    @Autowired
    RouteUtils routeUtils;

    @Autowired
    CamelContext camelContext;

    @Autowired
    StickySessionCacheManager stickySessionCacheManager;

    @Autowired
    ConsulCacheManager consulCacheManager;

    @Autowired
    MetricsProcessor metricsProcessor;

    WireMockServer wireMockServer;

    @Test
    void testGetAllServices() throws Exception {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/v1/catalog/services")).willReturn(aResponse().withBody(SERVICES_RESPONSE)));
        wireMockServer.stubFor(get(urlEqualTo("/v1/catalog/service/dummy")).willReturn(aResponse().withBody(SERVICE_DUMMY_RESPONSE)));

        Assertions.assertNotNull(camelContext);
        Assertions.assertNotNull(apiUtils);
        Assertions.assertNotNull(routeUtils);
        Assertions.assertNotNull(metricsProcessor);
        Assertions.assertNotNull(stickySessionCacheManager);
        Assertions.assertNotNull(consulCacheManager);

        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, apiUtils, routeUtils, metricsProcessor, stickySessionCacheManager, consulCacheManager);
        consulNodeDiscovery.setConsulHost("http://localhost:888");
        consulNodeDiscovery.setCapiContext("/capi/test");
        consulNodeDiscovery.processInfo();

        Thread.sleep(5000);
        Assertions.assertEquals(8, routeUtils.getAllActiveRoutes(camelContext).size());

        wireMockServer.stop();
    }
}