package io.surisoft.capi.lb.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsulObjectTest {

    private ConsulObject consulObject;

    @BeforeEach
    void setUp() {
        consulObject = new ConsulObject();
    }

    @Test
    void testID() {
        consulObject.setID("ID");
        assertEquals("ID", consulObject.getID());
    }

    @Test
    void getServiceName() {
        consulObject.setServiceName("name");
        assertEquals("name", consulObject.getServiceName());
    }

    @Test
    void getServiceAddress() {
        consulObject.setServiceAddress("address");
        assertEquals("address", consulObject.getServiceAddress());
    }

    @Test
    void getServicePort() {
        consulObject.setServicePort(80);
        assertEquals(80, consulObject.getServicePort());
    }

    @Test
    void getServiceMeta() {
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setGroup("dev");
        consulObject.setServiceMeta(serviceMeta);
        assertNotNull(consulObject.getServiceMeta());
        assertEquals("dev", consulObject.getServiceMeta().getGroup());
    }
}