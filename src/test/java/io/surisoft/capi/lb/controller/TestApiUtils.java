package io.surisoft.capi.lb.controller;

import io.surisoft.capi.lb.repository.ApiRepository;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.ConsulObject;
import io.surisoft.capi.lb.schema.HttpProtocol;
import io.surisoft.capi.lb.schema.Mapping;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.model.RouteDefinition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-persistence-application.properties"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestApiUtils {

    @Autowired
    ApiUtils apiUtils;

    @Autowired
    ApiRepository apiRepository;

    @Test
    @Order(1)
    void testGetApiId() {
        Api api = new Api();
        api.setName("unit-test");
        api.setContext("test-context");
        Assertions.assertEquals(apiUtils.getApiId(api), "unit-test:test-context");
    }

    @Test
    @Order(2)
    void testConsulObjectToMapping() {
        ConsulObject consulObject1 = new ConsulObject();
        consulObject1.setServiceAddress("localhost");
        consulObject1.setServicePort(9999);
        consulObject1.setServiceName("unit-test");
        List<String> serviceTags1 = new ArrayList<>();
        serviceTags1.add("test=unit");
        serviceTags1.add("no-root-context");
        consulObject1.setServiceTags(serviceTags1);

        Mapping mapping1 = apiUtils.consulObjectToMapping(consulObject1);
        Assertions.assertEquals(mapping1.getHostname(), "localhost");
        Assertions.assertEquals(mapping1.getRootContext(), "/");
        Assertions.assertEquals(mapping1.getPort(), 9999);

        ConsulObject consulObject2= new ConsulObject();
        consulObject2.setServiceAddress("localhost");
        consulObject2.setServicePort(8888);
        consulObject2.setServiceName("unit-test");
        List<String> serviceTags2 = new ArrayList<>();
        serviceTags2.add("test=unit");
        consulObject2.setServiceTags(serviceTags2);

        Mapping mapping2 = apiUtils.consulObjectToMapping(consulObject2);
        Assertions.assertEquals(mapping2.getHostname(), "localhost");
        Assertions.assertEquals(mapping2.getRootContext(), "/unit-test");
        Assertions.assertEquals(mapping2.getPort(), 8888);
    }

    @Test
    @Order(3)
    void testUpdateExistingApi1() {
        Api persistedApi = new Api();
        persistedApi.setName("persisted-api");
        persistedApi.setContext("persisted-context");
        persistedApi.setId(apiUtils.getApiId(persistedApi));
        persistedApi.setMappingList(getMappingList(1, "localhost"));

        Api incomingApi = new Api();
        incomingApi.setName("persisted-api");
        incomingApi.setContext("persisted-context");
        incomingApi.setId(apiUtils.getApiId(incomingApi));
        incomingApi.setMappingList(getMappingList(1, "new.domain"));

        apiRepository.save(persistedApi);
        apiUtils.updateExistingApi(persistedApi, incomingApi, apiRepository);

        Optional<Api> apiToCheck = apiRepository.findById(persistedApi.getId());
        Assertions.assertTrue(apiToCheck.isPresent());
        Assertions.assertEquals(apiToCheck.get().getMappingList().size(), 2);

        apiRepository.delete(persistedApi);
    }

    @Test
    @Order(4)
    void testUpdateExistingApi2() {
        Api persistedApi = new Api();
        persistedApi.setName("persisted-api");
        persistedApi.setContext("persisted-context");
        persistedApi.setId(apiUtils.getApiId(persistedApi));
        persistedApi.setMappingList(getMappingList(1, "localhost"));

        Api incomingApi = new Api();
        incomingApi.setName("persisted-api");
        incomingApi.setContext("persisted-context");
        incomingApi.setId(apiUtils.getApiId(incomingApi));
        incomingApi.setMappingList(getMappingList(2, "new.domain"));

        apiRepository.save(persistedApi);
        apiUtils.updateExistingApi(persistedApi, incomingApi, apiRepository);

        Optional<Api> apiToCheck = apiRepository.findById(persistedApi.getId());
        Assertions.assertTrue(apiToCheck.isPresent());
        Assertions.assertEquals(apiToCheck.get().getMappingList().size(), 2);

        apiRepository.delete(persistedApi);
    }

    private List<Mapping> getMappingList(int howManyMapping, String hostname) {
        List<Mapping> mappingList = new ArrayList<>();
        for(int i = 0; i < howManyMapping; i++) {
           Mapping mapping = new Mapping();
           mapping.setHostname(hostname + i);
           mapping.setPort(80);
           mapping.setRootContext("/");
           mappingList.add(mapping);
        }
        return mappingList;
    }

    private List<Mapping> changeMapping(List<Mapping> mappingList) {
        Mapping mapping = mappingList.get(0);
        mapping.setHostname("new.domain");
        return mappingList;
    }

}