package io.surisoft.capi.processor;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ServiceMeta;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.HttpUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.cache2k.Cache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(
        locations = "classpath:test-openapi-application.properties"
)
class OpenApiProcessorTest {

    private OpenAPI mockOpenAPI;

    private OpenApiProcessor openApiProcessorUnderTest;

    @Autowired
    HttpUtils httpUtils;

    @Autowired(required = false)
    OpaService opaService;

    @Autowired
    Cache<String, Service> serviceCache;

    @Autowired
    CamelContext camelContext;

    private String openApiDefinition = "openapi: \"3.0.0\"\n" +
            "info:\n" +
            "  version: 1.0.0\n" +
            "  title: Swagger Petstore\n" +
            "  license:\n" +
            "    name: MIT\n" +
            "servers:\n" +
            "  - url: http://petstore.swagger.io/v1\n" +
            "paths:\n" +
            "  /pets:\n" +
            "    get:\n" +
            "      summary: List all pets\n" +
            "      operationId: listPets\n" +
            "      tags:\n" +
            "        - pets\n" +
            "      parameters:\n" +
            "        - name: limit\n" +
            "          in: query\n" +
            "          description: How many items to return at one time (max 100)\n" +
            "          required: false\n" +
            "          schema:\n" +
            "            type: integer\n" +
            "            maximum: 100\n" +
            "            format: int32\n" +
            "      responses:\n" +
            "        '200':\n" +
            "          description: A paged array of pets\n" +
            "          headers:\n" +
            "            x-next:\n" +
            "              description: A link to the next page of responses\n" +
            "              schema:\n" +
            "                type: string\n" +
            "          content:\n" +
            "            application/json:    \n" +
            "              schema:\n" +
            "                $ref: \"#/components/schemas/Pets\"\n" +
            "        default:\n" +
            "          description: unexpected error\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: \"#/components/schemas/Error\"\n" +
            "    post:\n" +
            "      summary: Create a pet\n" +
            "      operationId: createPets\n" +
            "      tags:\n" +
            "        - pets\n" +
            "      responses:\n" +
            "        '201':\n" +
            "          description: Null response\n" +
            "        default:\n" +
            "          description: unexpected error\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: \"#/components/schemas/Error\"\n" +
            "  /pets/{petId}:\n" +
            "    get:\n" +
            "      summary: Info for a specific pet\n" +
            "      operationId: showPetById\n" +
            "      tags:\n" +
            "        - pets\n" +
            "      parameters:\n" +
            "        - name: petId\n" +
            "          in: path\n" +
            "          required: true\n" +
            "          description: The id of the pet to retrieve\n" +
            "          schema:\n" +
            "            type: string\n" +
            "      responses:\n" +
            "        '200':\n" +
            "          description: Expected response to a valid request\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: \"#/components/schemas/Pet\"\n" +
            "        default:\n" +
            "          description: unexpected error\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: \"#/components/schemas/Error\"\n" +
            "  /pet/secure/{petId}:\n" +
            "    get:\n" +
            "      summary: Info for a specific pet\n" +
            "      operationId: showPetById\n" +
            "      tags:\n" +
            "        - pets\n" +
            "      parameters:\n" +
            "        - name: petId\n" +
            "          in: path\n" +
            "          required: true\n" +
            "          description: The id of the pet to retrieve\n" +
            "          schema:\n" +
            "            type: string\n" +
            "      responses:\n" +
            "        '200':\n" +
            "          description: Expected response to a valid request\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: \"#/components/schemas/Pet\"\n" +
            "        default:\n" +
            "          description: unexpected error\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                $ref: \"#/components/schemas/Error\"              \n" +
            "      security:\n" +
            "        - bearerToken:          \n" +
            "components:\n" +
            "  schemas:\n" +
            "    Pet:\n" +
            "      type: object\n" +
            "      required:\n" +
            "        - id\n" +
            "        - name\n" +
            "      properties:\n" +
            "        id:\n" +
            "          type: integer\n" +
            "          format: int64\n" +
            "        name:\n" +
            "          type: string\n" +
            "        tag:\n" +
            "          type: string\n" +
            "    Pets:\n" +
            "      type: array\n" +
            "      maxItems: 100\n" +
            "      items:\n" +
            "        $ref: \"#/components/schemas/Pet\"\n" +
            "    Error:\n" +
            "      type: object\n" +
            "      required:\n" +
            "        - code\n" +
            "        - message\n" +
            "      properties:\n" +
            "        code:\n" +
            "          type: integer\n" +
            "          format: int32\n" +
            "        message:\n" +
            "          type: string";

    @BeforeEach
    void setUp() {

    }

    @Test
    void testProcessWithAValidPathWithoutPlaceholders() throws Exception {
        // Setup
        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI, httpUtils, serviceCache, opaService);

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setHeader("CamelHttpPath", "/pets");
        exchange.getIn().setHeader("CamelHttpMethod", "GET");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNull(exchange.getException());
    }

    @Test
    void testProcessWithAValidPathWithPlaceholders() throws Exception {
        // Setup
        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI, httpUtils, serviceCache, opaService);

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setHeader("CamelHttpPath", "/pets/123");
        exchange.getIn().setHeader("CamelHttpMethod", "GET");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNull(exchange.getException());
    }

    @Test
    void testProcessWithAValidSecuredPathAndPlaceHolder() throws Exception {
        // Setup
        WireMockRule opaEndpoint = new WireMockRule(9999);
        opaEndpoint.start();
        opaEndpoint.stubFor(post(urlEqualTo("/v1/data/capi/test/dev/allow")).willReturn(aResponse().withBody("""
                {
                    "result": true
                }""")));

        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI, httpUtils, serviceCache, opaService);

        Service service = new Service();
        service.setName("test");
        service.setContext("/test/dev");
        service.setId("test:dev");

        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setOpaRego("capi/test/dev");

        service.setServiceMeta(serviceMeta);
        serviceCache.put("test:dev", service);

        //CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjb2VsaHJvIiwiaHR0cHM6XC9cL2VjYXMuZWMuZXVyb3BhLmV1XC9jbGFpbXNcL2VtcGxveWVlX251bWJlciI6IjkwMTY3NTgzIiwic3Vic2NyaXB0aW9ucyI6WyJcL2NhcGkiXSwiaHR0cHM6XC9cL2VjYXMuZWMuZXVyb3BhLmV1XC9jbGFpbXNcL2RlcGFydG1lbnRfbnVtYmVyIjoiRElHSVQuQS40LjAwNSIsImlzcyI6Imh0dHBzOlwvXC9hcGkuYWNjZXB0YW5jZS50ZWNoLmVjLmV1cm9wYS5ldVwvZmVkZXJhdGlvblwvb2F1dGhcL3Rva2VuIiwiZ2l2ZW5fbmFtZSI6IlJvZHJpZ28iLCJjbGllbnRfaWQiOiIyTTRDM0pGWFcyUjZTaTQ3a0FZNDJkMWpmelRWV01KYlNzaWdmZWxmdEVIelpaaUZGNHpyME9ncUtjSWh0bVdrT3I1aG5temtHeVRXcXpHTnNPVDlLeXItcmE4Z25DRjFsV2FHOW5nUzNxdGM0eSIsImh0dHBzOlwvXC9lY2FzLmVjLmV1cm9wYS5ldVwvY2xhaW1zXC9vcmdfaWQiOiIyNTA2NjMiLCJhdWQiOiJwQWYzWWRWcmlMeVRSNXI4NGR2TUdMMENjOFVhIiwibmJmIjoxNzA0MzgwNDQzLCJwaG9uZU51bWJlciI6Ijk0MDE0IiwiYXpwIjoicEFmM1lkVnJpTHlUUjVyODRkdk1HTDBDYzhVYSIsIm5hbWUiOiJSb2RyaWdvIENPRUxITyIsImV4cCI6MTcwNDM4NDA0MywiaWF0IjoxNzA0MzgwNDQzLCJmYW1pbHlfbmFtZSI6IkNPRUxITyIsImp0aSI6IjhiNTEyNDNlLWU5NDItNGZkNS1iMTYwLWNiMzFjNTQ3ZmU0MyIsImVtYWlsIjoiUm9kcmlnby5DT0VMSE9AZXh0LmVjLmV1cm9wYS5ldSIsInVzZXJuYW1lIjoiY29lbGhybyJ9.F-Dy8OdjDSQsv19T4ET_j5lMLkie6q-q8UfxxcEkqx8gUAf-fykG6iBIl09PdGf-wZiMpNIIOSKukinWAneICl3oPqEmSIlfpnzc4yvY5OBpAKXDqUzvLBW7MKTf1sKiO9NW1fDQbpENBiEDLpezFV-P33ZWjNXU6MteTaM9Z8xqle-tAjNLF0HgFL-oIqWUEpOD9rDCsCKjmOC_pHCNGDz3Nol5KfRvBxpiTdGa_le_T0KVQOdX9hW3dqPFBtmfvzyiVA667BBioBHdC7F3njD-jjnoOTjXCjPiytOBXWYeI1n3EvLvkEtN6R5vQfUafTCVcBmDtoqGYzd-YC3tiw");
        exchange.getIn().setHeader("CamelHttpPath", "/pet/secure/123");
        exchange.getIn().setHeader("CamelServletContextPath", "/test/dev");
        exchange.getIn().setHeader("CamelHttpMethod", "GET");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNull(exchange.getException());

        serviceCache.clear();
        opaEndpoint.stop();
    }

    @Test
    void testProcessWithAInvalidSecuredPathAndPlaceHolder() throws Exception {
        // Setup
        WireMockRule opaEndpoint = new WireMockRule(9999);
        opaEndpoint.start();
        opaEndpoint.stubFor(post(urlEqualTo("/v1/data/capi/test/dev/allow")).willReturn(aResponse().withBody("""
                {
                    "result": false
                }""")));

        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI, httpUtils, serviceCache, opaService);

        Service service = new Service();
        service.setName("test");
        service.setContext("/test/dev");
        service.setId("test:dev");

        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setOpaRego("capi/test/dev");

        service.setServiceMeta(serviceMeta);
        serviceCache.put("test:dev", service);

        //CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjb2VsaHJvIiwiaHR0cHM6XC9cL2VjYXMuZWMuZXVyb3BhLmV1XC9jbGFpbXNcL2VtcGxveWVlX251bWJlciI6IjkwMTY3NTgzIiwic3Vic2NyaXB0aW9ucyI6WyJcL2NhcGkiXSwiaHR0cHM6XC9cL2VjYXMuZWMuZXVyb3BhLmV1XC9jbGFpbXNcL2RlcGFydG1lbnRfbnVtYmVyIjoiRElHSVQuQS40LjAwNSIsImlzcyI6Imh0dHBzOlwvXC9hcGkuYWNjZXB0YW5jZS50ZWNoLmVjLmV1cm9wYS5ldVwvZmVkZXJhdGlvblwvb2F1dGhcL3Rva2VuIiwiZ2l2ZW5fbmFtZSI6IlJvZHJpZ28iLCJjbGllbnRfaWQiOiIyTTRDM0pGWFcyUjZTaTQ3a0FZNDJkMWpmelRWV01KYlNzaWdmZWxmdEVIelpaaUZGNHpyME9ncUtjSWh0bVdrT3I1aG5temtHeVRXcXpHTnNPVDlLeXItcmE4Z25DRjFsV2FHOW5nUzNxdGM0eSIsImh0dHBzOlwvXC9lY2FzLmVjLmV1cm9wYS5ldVwvY2xhaW1zXC9vcmdfaWQiOiIyNTA2NjMiLCJhdWQiOiJwQWYzWWRWcmlMeVRSNXI4NGR2TUdMMENjOFVhIiwibmJmIjoxNzA0MzgwNDQzLCJwaG9uZU51bWJlciI6Ijk0MDE0IiwiYXpwIjoicEFmM1lkVnJpTHlUUjVyODRkdk1HTDBDYzhVYSIsIm5hbWUiOiJSb2RyaWdvIENPRUxITyIsImV4cCI6MTcwNDM4NDA0MywiaWF0IjoxNzA0MzgwNDQzLCJmYW1pbHlfbmFtZSI6IkNPRUxITyIsImp0aSI6IjhiNTEyNDNlLWU5NDItNGZkNS1iMTYwLWNiMzFjNTQ3ZmU0MyIsImVtYWlsIjoiUm9kcmlnby5DT0VMSE9AZXh0LmVjLmV1cm9wYS5ldSIsInVzZXJuYW1lIjoiY29lbGhybyJ9.F-Dy8OdjDSQsv19T4ET_j5lMLkie6q-q8UfxxcEkqx8gUAf-fykG6iBIl09PdGf-wZiMpNIIOSKukinWAneICl3oPqEmSIlfpnzc4yvY5OBpAKXDqUzvLBW7MKTf1sKiO9NW1fDQbpENBiEDLpezFV-P33ZWjNXU6MteTaM9Z8xqle-tAjNLF0HgFL-oIqWUEpOD9rDCsCKjmOC_pHCNGDz3Nol5KfRvBxpiTdGa_le_T0KVQOdX9hW3dqPFBtmfvzyiVA667BBioBHdC7F3njD-jjnoOTjXCjPiytOBXWYeI1n3EvLvkEtN6R5vQfUafTCVcBmDtoqGYzd-YC3tiw");
        exchange.getIn().setHeader("CamelHttpPath", "/pet/secure/123");
        exchange.getIn().setHeader("CamelServletContextPath", "/test/dev");
        exchange.getIn().setHeader("CamelHttpMethod", "GET");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNotNull(exchange.getException());

        serviceCache.clear();
        opaEndpoint.stop();
    }

    @Test
    void testProcessWithAnInvalidPath() throws Exception {
        // Setup
        mockOpenAPI = new OpenAPIV3Parser().readContents(openApiDefinition).getOpenAPI();
        openApiProcessorUnderTest = new OpenApiProcessor(mockOpenAPI, httpUtils, serviceCache, opaService);

        CamelContext ctx = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(ctx);
        exchange.getIn().setHeader("CamelHttpPath", "/pet/123");
        exchange.getIn().setHeader("CamelHttpMethod", "PUT");


        // Run the test
        openApiProcessorUnderTest.process(exchange);

        Assertions.assertNotNull(exchange.getException());
    }
}
