package io.surisoft.capi.lb.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
      locations = "classpath:test-persistence-application.properties"
)
class TestLoadBalancer {

    private static final String THE_GOOD_API = """
            {
                 "name": "unit-test-api",
                 "context": "test",
                 "mappingList": [
                        {
                            "hostname": "localhost",
                            "port": 8881,
                            "rootContext": "/",
                            "ingress": false
                        },
                        {
                            "hostname": "localhost",
                            "port": 8882,
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
        WireMockServer deployedNode1 = new WireMockServer(8881);
        WireMockServer deployedNode2 = new WireMockServer(8882);
        deployedNode1.start();
        deployedNode2.start();
        deployedNode1.stubFor(get(urlEqualTo("/node")).willReturn(aResponse().withBody(NODE_1_RESPONSE)));
        deployedNode2.stubFor(get(urlEqualTo("/node")).willReturn(aResponse().withBody(NODE_2_RESPONSE)));


        mockMvc.perform(MockMvcRequestBuilders.post("/manager/api/register/node").content(THE_GOOD_API)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        //Wait for API do be deployed
        Thread.sleep(5000);

        ResponseEntity<String> responseFromNode1 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertEquals(responseFromNode1.getStatusCode(), HttpStatus.OK);
        Assertions.assertTrue(responseFromNode1.getBody().equals(NODE_1_RESPONSE) || responseFromNode1.getBody().equals(NODE_2_RESPONSE));

        String firstResponse = responseFromNode1.getBody();

        ResponseEntity<String> responseFromNode2 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertEquals(responseFromNode2.getStatusCode(), HttpStatus.OK);

        //If the API is correctly load balanced the response should be different
        Assertions.assertNotEquals(responseFromNode2.getBody(), firstResponse);

        deployedNode1.stop();
        deployedNode2.stop();
    }

    @Test
    void testDeployApiAndTestLoadBalancerFailOverNode2() throws Exception {

        //Start mock load balanced nodes
        WireMockServer deployedNode1 = new WireMockServer(8881);
        deployedNode1.start();
        deployedNode1.stubFor(get(urlEqualTo("/node")).willReturn(aResponse().withBody(NODE_1_RESPONSE)));


        mockMvc.perform(MockMvcRequestBuilders.post("/manager/api/register/node").content(THE_GOOD_API)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        //Wait for API do be deployed
        Thread.sleep(5000);

        ResponseEntity<String> responseFromNode1 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertEquals(responseFromNode1.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(responseFromNode1.getBody(), NODE_1_RESPONSE);

        ResponseEntity<String> responseFromNode2 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertEquals(responseFromNode2.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(responseFromNode2.getBody(), NODE_1_RESPONSE);

        deployedNode1.stop();
    }

    @Test
    void testDeployApiAndTestLoadBalancerFailOverNode1() throws Exception {

        //Start mock load balanced nodes
        WireMockServer deployedNode2 = new WireMockServer(8882);
        deployedNode2.start();
        deployedNode2.stubFor(get(urlEqualTo("/node")).willReturn(aResponse().withBody(NODE_2_RESPONSE)));


        mockMvc.perform(MockMvcRequestBuilders.post("/manager/api/register/node").content(THE_GOOD_API)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        //Wait for API do be deployed
        Thread.sleep(5000);

        ResponseEntity<String> responseFromNode1 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertEquals(responseFromNode1.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(responseFromNode1.getBody(), NODE_2_RESPONSE);

        ResponseEntity<String> responseFromNode2 = restTemplate.getForEntity("/capi/test/node", String.class);
        Assertions.assertEquals(responseFromNode2.getStatusCode(), HttpStatus.OK);
        Assertions.assertEquals(responseFromNode2.getBody(), NODE_2_RESPONSE);

        deployedNode2.stop();
    }
}