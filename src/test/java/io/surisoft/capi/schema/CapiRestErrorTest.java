package io.surisoft.capi.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapiRestErrorTest {

    private CapiRestError capiRestError;

    @BeforeEach
    void setUp() {
        capiRestError = new CapiRestError();
    }

    @Test
    void testRouteID() {
        capiRestError.setRouteID("ID");
        assertEquals("ID", capiRestError.getRouteID());
    }

    @Test
    void testErrorMessage() {
        capiRestError.setErrorMessage("error");
        assertEquals("error", capiRestError.getErrorMessage());
    }

    @Test
    void testErrorCode() {
        capiRestError.setErrorCode(400);
        assertEquals(400, capiRestError.getErrorCode());
    }

    @Test
    void testHttpUri() {
        capiRestError.setHttpUri("/unit/test");
        assertEquals("/unit/test", capiRestError.getHttpUri());
    }

    @Test
    void testZipkinTraceID() {
        capiRestError.setTraceID("ID");
        assertEquals("ID", capiRestError.getTraceID());
    }
}