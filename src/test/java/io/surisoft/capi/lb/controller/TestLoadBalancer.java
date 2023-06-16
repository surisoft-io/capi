package io.surisoft.capi.lb.controller;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestLoadBalancer {

    private static final String THE_GOOD_API = """
            {
                 "name": "unit-test-api",
                 "context": "test",
                 "mappingList": [
                        {
                            "hostname": "localhost",
                            "port": 1,
                            "rootContext": "/",
                            "ingress": false
                        },
                        {
                            "hostname": "localhost",
                            "port": 2,
                            "rootContext": "/",
                            "ingress": false
                        }
                 ],
                 "roundRobinEnabled": true,
                 "failoverEnabled": true,
                 "matchOnUriPrefix": true,
                 "httpMethod": "ALL",
                 "httpProtocol": "HTTP",
                 "removeMe": false
            }""";

    private static final String NODE_1_RESPONSE = "OK NODE 1";
    private static final String NODE_2_RESPONSE = "OK NODE 2";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void initialize() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testDeployApiAndTestLoadBalancer() throws Exception {

        //Start mock load balanced nodes
        WireMockRule deployedNode1 = new WireMockRule(wireMockConfig().dynamicPort());
        WireMockRule deployedNode2 = new WireMockRule(wireMockConfig().dynamicPort());

        deployedNode1.start();
        deployedNode2.start();

        deployedNode1.stubFor(get(urlEqualTo("/node")).willReturn(aResponse().withBody(NODE_1_RESPONSE)));
        deployedNode2.stubFor(get(urlEqualTo("/node")).willReturn(aResponse().withBody(NODE_2_RESPONSE)));

        String apiDefinition = THE_GOOD_API.replace("1", String.valueOf(deployedNode1.port()));
        apiDefinition = apiDefinition.replace("2", String.valueOf(deployedNode2.port()));

        mockMvc.perform(MockMvcRequestBuilders.post("/manager/register/node").content(apiDefinition)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        //Wait for API do be deployed
        Thread.sleep(5000);

        ResponseEntity<String> responseFromNode1 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertTrue(responseFromNode1.getStatusCode().is2xxSuccessful());
        Assertions.assertTrue(Objects.equals(responseFromNode1.getBody(), NODE_1_RESPONSE) || Objects.equals(responseFromNode1.getBody(), NODE_2_RESPONSE));

        String firstResponse = responseFromNode1.getBody();

        ResponseEntity<String> responseFromNode2 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertTrue(responseFromNode2.getStatusCode().is2xxSuccessful());

        //If the API is correctly load balanced the response should be different
        Assertions.assertNotEquals(responseFromNode2.getBody(), firstResponse);

        //Stopping Node 2 to test the fail-over
        deployedNode2.stop();

        ResponseEntity<String> failOverResponse1 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertTrue(failOverResponse1.getStatusCode().is2xxSuccessful());
        Assertions.assertEquals(failOverResponse1.getBody(), NODE_1_RESPONSE);

        ResponseEntity<String> failOverResponse2 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertTrue(failOverResponse2.getStatusCode().is2xxSuccessful());
        Assertions.assertEquals(failOverResponse2.getBody(), NODE_1_RESPONSE);

        //Stopping mocks
        deployedNode2.stop();
        deployedNode1.stop();
    }
}