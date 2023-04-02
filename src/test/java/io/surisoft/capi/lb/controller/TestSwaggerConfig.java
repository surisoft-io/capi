package io.surisoft.capi.lb.controller;

import io.surisoft.capi.lb.configuration.SwaggerConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import org.junit.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(
      locations = "classpath:test-swagger-application.properties"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSwaggerConfig {

    @Mock
    SwaggerConfig swaggerConfigUnderTest;

    @Autowired
    OpenAPI openAPI;

    @Test
    public void testGenerateOpenAPI() {
        // Setup
        final OpenAPI expectedResult = new OpenAPI(SpecVersion.V30);

        // Run the test
        final OpenAPI result = swaggerConfigUnderTest.generateOpenAPI();

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testPublicApi() {
        // Setup
        // Run the test
        final GroupedOpenApi result = swaggerConfigUnderTest.publicApi();

        // Verify the results
    }

/*@Autowired
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
    }*/
}