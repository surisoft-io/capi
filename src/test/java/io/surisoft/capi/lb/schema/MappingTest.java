package io.surisoft.capi.lb.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MappingTest {

    Mapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new Mapping();
    }

    @Test
    void testRootContext() {
        mapping.setRootContext("/");
        assertEquals("/", mapping.getRootContext());
    }

    @Test
    void testHostname() {
        mapping.setHostname("localhost");
        assertEquals("localhost", mapping.getHostname());
    }

    @Test
    void testPort() {
        mapping.setPort(80);
        assertEquals(80, mapping.getPort());
    }

    @Test
    void testIngress() {
        mapping.setIngress(true);
        assertTrue(mapping.isIngress());
    }

    @Test
    void testEquals() {
        mapping.setIngress(true);
        mapping.setHostname("localhost");
        mapping.setPort(80);
        mapping.setRootContext("/");

        Mapping mapping2 = new Mapping();
        mapping2.setIngress(true);
        mapping2.setHostname("localhost");
        mapping2.setPort(43);
        mapping2.setRootContext("/");

        assertNotEquals(mapping, mapping2);
    }

    @Test
    void testTenandId() {
        mapping.setTenandId("123");
        assertEquals("123", mapping.getTenandId());
    }
}