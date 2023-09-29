package io.surisoft.capi.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.ConsulNodeDiscovery;
import io.surisoft.capi.utils.RouteUtils;
import io.surisoft.capi.utils.ServiceUtils;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-consul-application.properties"
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
                "ServiceTags": [],
                "ServiceAddress": "localhost",
                "ServiceWeights": {
                  "Passing": 1,
                  "Warning": 1
                },
                "ServiceMeta": {
                   "group": "dev"
                },
                "ServicePort": 8081,
                "ServiceSocketPath": "",
                "ServiceEnableTagOverride": false
              }
            ]""";

    @Autowired
    ServiceUtils serviceUtils;

    @Autowired
    RouteUtils routeUtils;

    @Autowired
    CamelContext camelContext;

    @Autowired(required = false)
    StickySessionCacheManager stickySessionCacheManager;

    @Autowired
    Cache<String, Service> serviceCache;

    @Autowired
    MetricsProcessor metricsProcessor;

    @Autowired(required = false)
    Map<String, WebsocketClient> websocketClientMap;

    WireMockServer wireMockServer;

    @Test
    void testGetAllServices() throws Exception {
        WireMockRule wireMockServer = new WireMockRule(wireMockConfig().dynamicPort());
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/v1/catalog/services")).willReturn(aResponse().withBody(SERVICES_RESPONSE)));
        wireMockServer.stubFor(get(urlEqualTo("/v1/catalog/service/dummy")).willReturn(aResponse().withBody(SERVICE_DUMMY_RESPONSE)));

        Assertions.assertNotNull(camelContext);
        Assertions.assertNotNull(serviceUtils);
        Assertions.assertNotNull(routeUtils);
        Assertions.assertNotNull(metricsProcessor);
        Assertions.assertNotNull(serviceCache);

        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, serviceUtils, routeUtils, metricsProcessor, serviceCache, websocketClientMap);
        consulNodeDiscovery.setStickySessionCacheManager(stickySessionCacheManager);
        consulNodeDiscovery.setConsulHost("http://localhost:" + wireMockServer.port());
        consulNodeDiscovery.setCapiContext("/capi/test");
        consulNodeDiscovery.processInfo();

        Thread.sleep(5000);
        Assertions.assertEquals(10, routeUtils.getAllActiveRoutes(camelContext).size());

        wireMockServer.stop();
    }
}