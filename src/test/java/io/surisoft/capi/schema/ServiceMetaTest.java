package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceMetaTest {

    private ServiceMeta serviceMeta;
    @BeforeEach
    void setUp() {
        serviceMeta = new ServiceMeta();
    }

    @Test
    void testSecured() {
        serviceMeta.setSecured(true);
        assertTrue(serviceMeta.isSecured());
    }

    @Test
    void testRootContext() {
        serviceMeta.setRootContext("/");
        assertEquals("/", serviceMeta.getRootContext());
    }

    @Test
    void testSchema() {
        serviceMeta.setSchema("http");
        assertEquals("http", serviceMeta.getSchema());
    }

    @Test
    void testTenantAware() {
        serviceMeta.setTenantAware(true);
        assertTrue(serviceMeta.isTenantAware());
    }

    @Test
    void testTenantId() {
        serviceMeta.setTenantId("ID");
        assertEquals("ID", serviceMeta.getTenantId());
    }

    @Test
    void testGroup() {
        serviceMeta.setGroup("dev");
        assertEquals("dev", serviceMeta.getGroup());
    }

    @Test
    void testB3TraceId() {
        serviceMeta.setB3TraceId(true);
        assertTrue(serviceMeta.isB3TraceId());
    }

    @Test
    void testIngress() {
        serviceMeta.setIngress("ingress");
        assertEquals("ingress", serviceMeta.getIngress());
    }

    @Test
    void testOpenApiEndpoint() {
        serviceMeta.setOpenApiEndpoint("http://openapi.com");
        assertEquals("http://openapi.com", serviceMeta.getOpenApiEndpoint());
    }

    @Test
    void testStickySessionEnabled() {
        serviceMeta.setStickySession(true);
        assertTrue(serviceMeta.isStickySession());
    }

    @Test
    void testStickySessionType() {
        serviceMeta.setStickySessionType("cookie");
        assertEquals("cookie", serviceMeta.getStickySessionType());
    }

    @Test
    void testStickySessionKey() {
        serviceMeta.setStickySessionKey("SESSION");
        assertEquals("SESSION", serviceMeta.getStickySessionKey());
    }

    @Test
    void testOpaRego() {
        serviceMeta.setOpaRego("capi/smk_api/dev");
        assertEquals("capi/smk_api/dev", serviceMeta.getOpaRego());
    }
}