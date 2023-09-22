package io.surisoft.capi.controller;

import io.surisoft.capi.schema.ConsulObject;
import io.surisoft.capi.schema.Mapping;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ServiceMeta;
import io.surisoft.capi.utils.ServiceUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestServiceUtils {

    @Autowired
    ServiceUtils serviceUtils;

    @Test
    @Order(1)
    void testGetServiceId() {
        Service service = new Service();

        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setGroup("test");
        service.setServiceMeta(serviceMeta);

        service.setName("unit-test");
        service.setContext("test-context");
        Assertions.assertEquals(serviceUtils.getServiceId(service), "unit-test:test");
    }

    @Test
    @Order(2)
    void testConsulObjectToMapping() {
        ConsulObject consulObject1 = new ConsulObject();
        consulObject1.setServiceAddress("localhost");
        consulObject1.setServicePort(9999);
        consulObject1.setServiceName("unit-test");
        ServiceMeta serviceMeta = new ServiceMeta();
        consulObject1.setServiceMeta(serviceMeta);

        Mapping mapping1 = serviceUtils.consulObjectToMapping(consulObject1);
        Assertions.assertEquals(mapping1.getHostname(), "localhost");
        Assertions.assertEquals(mapping1.getRootContext(), "/");
        Assertions.assertEquals(mapping1.getPort(), 9999);

        ConsulObject consulObject2= new ConsulObject();
        consulObject2.setServiceAddress("localhost");
        consulObject2.setServicePort(8888);
        consulObject2.setServiceName("unit-test");
        ServiceMeta serviceMeta2 = new ServiceMeta();
        serviceMeta2.setRootContext("unit-test");
        consulObject2.setServiceMeta(serviceMeta2);

        Mapping mapping2 = serviceUtils.consulObjectToMapping(consulObject2);
        Assertions.assertEquals(mapping2.getHostname(), "localhost");
        Assertions.assertEquals(mapping2.getRootContext(), "/unit-test");
        Assertions.assertEquals(mapping2.getPort(), 8888);
    }
}