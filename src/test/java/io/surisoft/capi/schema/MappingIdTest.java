package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MappingIdTest {

    private MappingId mappingId;

    @BeforeEach
    void setUp() {
        mappingId = new MappingId();
    }

    @Test
    void testRootContext() {
        mappingId.setRootContext("/");
        assertEquals("/", mappingId.getRootContext());
    }

    @Test
    void testHostname() {
        mappingId.setHostname("localhost");
        assertEquals("localhost", mappingId.getHostname());
    }

    @Test
    void getPort() {
        mappingId.setPort(1);
        assertEquals(1, mappingId.getPort());
    }
}