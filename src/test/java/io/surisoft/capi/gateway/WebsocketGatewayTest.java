package io.surisoft.capi.gateway;

import io.surisoft.capi.schema.Mapping;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.tracer.CapiGatewayTracer;
import io.surisoft.capi.utils.WebsocketUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class WebsocketGatewayTest {

    @Mock
    private WebsocketUtils mockWebsocketUtils;
    @Mock
    private CapiGatewayTracer mockTracer;

    private WebsocketGateway gatewayUnderTest;

    @BeforeEach
    void setUp() {
        gatewayUnderTest = new WebsocketGateway(
                8080,
                new HashMap<>(),
                mockWebsocketUtils,
                Optional.empty(),
                Optional.of(mockTracer),
                List.of("Authorization"),
                "oauth2_cookie"
        );
    }

    @Test
    void testSelectBackendRoundRobin() {
        WebsocketClient client = createClientWithMappings(
                createMapping("backend1", 8080),
                createMapping("backend2", 8081)
        );
        AtomicInteger counter = new AtomicInteger(0);

        URI first = gatewayUnderTest.selectBackend(client, counter);
        URI second = gatewayUnderTest.selectBackend(client, counter);
        URI third = gatewayUnderTest.selectBackend(client, counter);

        assertThat(first.getHost()).isEqualTo("backend1");
        assertThat(second.getHost()).isEqualTo("backend2");
        assertThat(third.getHost()).isEqualTo("backend1");
    }

    @Test
    void testSelectBackendReturnsNullForEmptyMappings() {
        WebsocketClient client = new WebsocketClient();
        client.setMappingList(new LinkedHashSet<>());
        AtomicInteger counter = new AtomicInteger(0);

        URI result = gatewayUnderTest.selectBackend(client, counter);
        assertThat(result).isNull();
    }

    @Test
    void testSelectBackendHandlesHostnameWithSchemePrefix() {
        WebsocketClient client = createClientWithMappings(
                createMapping("https://backend1", 443)
        );
        AtomicInteger counter = new AtomicInteger(0);

        URI result = gatewayUnderTest.selectBackend(client, counter);
        assertThat(result.toString()).isEqualTo("https://backend1:443");
    }

    @Test
    void testSelectBackendHandlesHostnameWithoutSchemePrefix() {
        WebsocketClient client = createClientWithMappings(
                createMapping("backend1", 8080)
        );
        AtomicInteger counter = new AtomicInteger(0);

        URI result = gatewayUnderTest.selectBackend(client, counter);
        assertThat(result.getScheme()).isEqualTo("http");
        assertThat(result.getHost()).isEqualTo("backend1");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    void testIsValidOriginWithValidUrl() {
        assertThat(gatewayUnderTest.isValidOrigin("http://example.com")).isTrue();
        assertThat(gatewayUnderTest.isValidOrigin("https://example.com:8443/path")).isTrue();
    }

    @Test
    void testIsValidOriginWithInvalidUrl() {
        assertThat(gatewayUnderTest.isValidOrigin("not-a-url")).isFalse();
        assertThat(gatewayUnderTest.isValidOrigin("")).isFalse();
    }

    @Test
    void testStopWorksWhenServerIsNull() {
        // server is null because runProxy() was never called
        assertThatCode(() -> gatewayUnderTest.stop()).doesNotThrowAnyException();
    }

    private WebsocketClient createClientWithMappings(Mapping... mappings) {
        WebsocketClient client = new WebsocketClient();
        Set<Mapping> mappingSet = new LinkedHashSet<>(Arrays.asList(mappings));
        client.setMappingList(mappingSet);
        return client;
    }

    private Mapping createMapping(String hostname, int port) {
        Mapping mapping = new Mapping();
        mapping.setHostname(hostname);
        mapping.setPort(port);
        mapping.setRootContext("/");
        return mapping;
    }
}
