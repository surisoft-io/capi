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

import java.util.ArrayList;
import java.util.List;

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

    @Test
    void testValidateServiceType() {
        final Service service = new Service();
        final ServiceMeta serviceMeta = new ServiceMeta();
        service.setServiceMeta(serviceMeta);
        serviceUtils.validateServiceType(service);
        Assertions.assertEquals("rest", service.getServiceMeta().getType());
    }

    @Test
    void testIsMappingChanged() {
        List<Mapping> list1 = new ArrayList<>();
        List<Mapping> list2 = new ArrayList<>();

        Mapping mapping1 = new Mapping();
        mapping1.setHostname("localhost");
        mapping1.setPort(80);
        mapping1.setRootContext("/");

        list1.add(mapping1);
        list2.add(mapping1);

        Assertions.assertFalse(serviceUtils.isMappingChanged(list1, list2));

        Mapping mapping2 = new Mapping();
        mapping2.setHostname("localhost");
        mapping2.setPort(443);
        mapping2.setRootContext("/");

        list1.add(mapping2);

        Assertions.assertTrue(serviceUtils.isMappingChanged(list1, list2));

        list1.remove(mapping1);

        Assertions.assertTrue(serviceUtils.isMappingChanged(list1, list2));
    }
}