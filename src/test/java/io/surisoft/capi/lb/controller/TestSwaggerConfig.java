package io.surisoft.capi.lb.controller;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-swagger-application.properties"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestSwaggerConfig {

    @Autowired
    OpenAPI openAPI;

    @Autowired
    GroupedOpenApi groupedOpenApi;

    @Test
    @Order(1)
    void testSwaggerConfig() {
        Assertions.assertNotNull(openAPI);
        Assertions.assertNotNull(groupedOpenApi);
        Assertions.assertEquals(openAPI.getInfo().getDescription(), "Management endpoint");
        Assertions.assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearerAuth"));
        Assertions.assertEquals(groupedOpenApi.getGroup(), "manager-endpoint");
        Assertions.assertTrue(groupedOpenApi.getPathsToMatch().contains("/manager/**"));
    }
}