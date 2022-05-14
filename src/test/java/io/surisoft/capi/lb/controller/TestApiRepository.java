package io.surisoft.capi.lb.controller;

import io.surisoft.capi.lb.repository.ApiRepository;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.Mapping;
import io.surisoft.capi.lb.utils.ApiUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-persistence-application.properties"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestApiRepository {

    @Autowired
    ApiRepository apiRepository;

    @Autowired
    ApiUtils apiUtils;

    @Test
    @Order(1)
    void testSaveApi() {
        Api api = new Api();
        api.setName("unit-test");
        api.setContext("unit-test-context");
        api.setId(apiUtils.getApiId(api));
        api.setMatchOnUriPrefix(true);
        api.setPublished(true);
        apiRepository.save(api);

        Optional<Api> savedApi = apiRepository.findById(apiUtils.getApiId(api));
        Assertions.assertTrue(savedApi.isPresent());
    }

    @Test
    @Order(2)
    void testGetAll() {
        Api api = new Api();
        api.setName("unit-test-2");
        api.setContext("unit-test-2-context");
        api.setPublished(false);
        api.setId(apiUtils.getApiId(api));
        api.setMatchOnUriPrefix(true);
        apiRepository.save(api);

        Optional<Api> savedApi = apiRepository.findById(apiUtils.getApiId(api));
        Assertions.assertTrue(savedApi.isPresent());

        Collection<Api> apiList = apiRepository.findAll();
        Assertions.assertEquals(apiList.size(), 2);
    }

    @Test
    @Order(3)
    void testFindByPublished() {
        Collection<Api> unpublishedApu = apiRepository.findByPublished(false);
        Assertions.assertTrue(unpublishedApu.size() == 1);
    }

    @Test
    @Order(4)
    void findAndDelete() {
        Optional<Api> apiToDelete = apiRepository.findById("unit-test:unit-test-context");
        Assertions.assertTrue(apiToDelete.isPresent());
        apiRepository.delete(apiToDelete.get());
        Collection<Api> apiList = apiRepository.findAll();
        Assertions.assertEquals(apiList.size(), 1);
    }

    @Test
    @Order(5)
    void findAndUpdate() {
        Optional<Api> apiToUpdate = apiRepository.findById("unit-test-2:unit-test-2-context");
        Assertions.assertTrue(apiToUpdate.isPresent());
        Assertions.assertTrue(apiToUpdate.get().getMappingList().isEmpty());

        List<Mapping> mappingList = new ArrayList<>();
        Mapping mapping = new Mapping();
        mapping.setHostname("localhost");
        mapping.setPort(80);
        mapping.setRootContext("/");

        mappingList.add(mapping);
        apiToUpdate.get().setMappingList(mappingList);
        apiRepository.update(apiToUpdate.get());

        Optional<Api> updatedApi = apiRepository.findById("unit-test-2:unit-test-2-context");
        Assertions.assertTrue(updatedApi.isPresent());
        Assertions.assertFalse(updatedApi.get().getMappingList().isEmpty());
    }
}