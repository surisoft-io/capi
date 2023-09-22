package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTest {

    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service();
        ServiceMeta serviceMeta = new ServiceMeta();
        service.setServiceMeta(serviceMeta);
    }

    @Test
    void testSecured() {
        service.getServiceMeta().setSecured(true);
        assertTrue(service.getServiceMeta().isSecured());
    }

    @Test
    void testId() {
        service.setId("ID");
        assertEquals("ID", service.getId());
    }

    @Test
    void testName() {
        service.setName("name");
        assertEquals("name", service.getName());
    }

    @Test
    void testContext() {
        service.setContext("context");
        assertEquals("context", service.getContext());
    }

    @Test
    void testMappingList() {
        Mapping mapping = new Mapping();
        Set<Mapping> mappingList = new HashSet<>();
        mappingList.add(mapping);
        service.setMappingList(mappingList);
        assertEquals(1, service.getMappingList().size());
    }

    @Test
    void testRoundRobinEnabled() {
        service.setRoundRobinEnabled(true);
        assertTrue(service.isRoundRobinEnabled());
    }

    @Test
    void testFailOverEnabled() {
        service.setFailOverEnabled(true);
        assertTrue(service.isFailOverEnabled());
    }

    @Test
    void testMatchOnUriPrefix() {
        service.setMatchOnUriPrefix(true);
        assertTrue(service.isMatchOnUriPrefix());
    }

    @Test
    void testHttpProtocol() {
        service.getServiceMeta().setSchema(HttpProtocol.HTTP.getProtocol());
        assertEquals(HttpProtocol.HTTP.getProtocol(), service.getServiceMeta().getSchema());
    }

    @Test
    void testStickySession() {
        service.getServiceMeta().setStickySession(true);
        assertTrue(service.getServiceMeta().isStickySession());
    }

    @Test
    void testStickySessionParam() {
        service.getServiceMeta().setStickySessionKey("param");
        assertEquals("param", service.getServiceMeta().getStickySessionKey());
    }

    @Test
    void testStickySessionParamInCookie() {
        service.getServiceMeta().setStickySessionType("cookie");
        assertEquals("cookie", service.getServiceMeta().getStickySessionType());
    }

    @Test
    void testTenantAware() {
        service.getServiceMeta().setTenantAware(true);
        assertTrue(service.getServiceMeta().isTenantAware());
    }
}