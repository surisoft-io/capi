package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {

    private Api api;

    @BeforeEach
    void setUp() {
        api = new Api();
    }

    @Test
    void testSecured() {
        api.setSecured(true);
        assertTrue(api.isSecured());
    }

    @Test
    void testId() {
        api.setId("ID");
        assertEquals("ID", api.getId());
    }

    @Test
    void getRouteId() {
        api.setRouteId("route:id");
        assertEquals("route:id", api.getRouteId());
    }

    @Test
    void testName() {
        api.setName("name");
        assertEquals("name", api.getName());
    }

    @Test
    void testContext() {
        api.setContext("context");
        assertEquals("context", api.getContext());
    }

    @Test
    void testMappingList() {
        Mapping mapping = new Mapping();
        Set<Mapping> mappingList = new HashSet<>();
        mappingList.add(mapping);
        api.setMappingList(mappingList);
        assertEquals(1, api.getMappingList().size());
    }

    @Test
    void testRoundRobinEnabled() {
        api.setRoundRobinEnabled(true);
        assertTrue(api.isRoundRobinEnabled());
    }

    @Test
    void testFailoverEnabled() {
        api.setFailoverEnabled(true);
        assertTrue(api.isFailoverEnabled());
    }

    @Test
    void testMatchOnUriPrefix() {
        api.setMatchOnUriPrefix(true);
        assertTrue(api.isMatchOnUriPrefix());
    }

    @Test
    void testHttpMethod() {
        api.setHttpMethod(HttpMethod.GET);
        assertEquals(HttpMethod.GET, api.getHttpMethod());
    }

    @Test
    void testHttpProtocol() {
        api.setHttpProtocol(HttpProtocol.HTTP);
        assertEquals(HttpProtocol.HTTP, api.getHttpProtocol());
    }

    @Test
    void testSwaggerEndpoint() {
        api.setSwaggerEndpoint("http://domain");
        assertEquals("http://domain", api.getSwaggerEndpoint());
    }

    @Test
    void testMaximumFailoverAttempts() {
        api.setMaximumFailoverAttempts(1);
        assertEquals(1, api.getMaximumFailoverAttempts());
    }

    @Test
    void testStickySession() {
        api.setStickySession(true);
        assertTrue(api.isStickySession());
    }

    @Test
    void testStickySessionParam() {
        api.setStickySessionParam("param");
        assertEquals("param", api.getStickySessionParam());
    }

    @Test
    void testStickySessionParamInCookie() {
        api.setStickySessionParamInCookie(true);
        assertTrue(api.isStickySessionParamInCookie());
    }

    @Test
    void testRemoveMe() {
        api.setRemoveMe(true);
        assertTrue(api.isRemoveMe());
    }

    @Test
    void testPublished() {
        api.setPublished(true);
        assertTrue(api.isPublished());
    }

    @Test
    void testForwardPrefix() {
        api.setForwardPrefix(true);
        assertTrue(api.isForwardPrefix());
    }

    @Test
    void testZipkinShowTraceId() {
        api.setZipkinShowTraceId(true);
        assertTrue(api.isZipkinShowTraceId());
    }

    @Test
    void testZipkinServiceName() {
        api.setZipkinServiceName("service");
        assertEquals("service", api.getZipkinServiceName());
    }

    @Test
    void testAuthorizationEndpointPublicKey() {
        api.setAuthorizationEndpointPublicKey("http://domain");
        assertEquals("http://domain", api.getAuthorizationEndpointPublicKey());
    }

    @Test
    void testTenantAware() {
        api.setTenantAware(true);
        assertTrue(api.isTenantAware());
    }
}