package io.surisoft.capi.lb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.schema.Api;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestApiManager {

    private static final String GOOD_API = """
            {
                 "name": "unit-test-api",
                 "context": "unit-test-api",
                 "mappingList": [
                        {
                            "hostname": "localhost",
                            "port": 8080,
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

    private static final String GOOD_API_NEW_MAPPING = """
            {
                 "name": "unit-test-api",
                 "context": "unit-test-api",
                 "mappingList": [
                        {
                            "hostname": "localhost",
                            "port": 8081,
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

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void initialize() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @Order(1)
    void testGetEmptyApi() throws Exception {
        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/manager/configured")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Api> apiList = objectMapper.readValue(getResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, Api.class));
        if(!apiList.isEmpty()) {
            System.out.println("-------------------------------------------------------");
            System.out.println(apiList.get(0).getId());
            System.out.println("-------------------------------------------------------");
        }
        Assertions.assertTrue(apiList.isEmpty());
    }

    @Test
    @Order(2)
    void testRegisterNodeBadRequest() throws Exception {
        Api api = new Api();
        api.setName("unit-test");
        api.setContext("/unit-test");

        mockMvc.perform(MockMvcRequestBuilders.post("/manager/register/node", objectMapper.writeValueAsString(api))
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void testRegisterNodeWithSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/manager/register/node").content(GOOD_API)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void testGetConfiguredApi() throws Exception {
        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/manager/configured")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Api> apiList = objectMapper.readValue(getResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, Api.class));
        Assertions.assertEquals(1, apiList.size());
        Assertions.assertEquals(1, apiList.get(0).getMappingList().size());
    }

    @Test
    @Order(5)
    void testRegisterNodeForSameApiWithSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/manager/register/node").content(GOOD_API_NEW_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    void testGetConfiguredApiWithNewMapping() throws Exception {
        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/manager/configured")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Api> apiList = objectMapper.readValue(getResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, Api.class));
        Assertions.assertEquals(1, apiList.size());
        Assertions.assertEquals(2, apiList.get(0).getMappingList().size());
    }

    @Test
    @Order(7)
    void removeDeplyedApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/manager/unregister/node").content(GOOD_API_NEW_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    void testEmptyResultAfterUndeploy() throws Exception {
        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/manager/configured")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Api> apiList = objectMapper.readValue(getResult.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, Api.class));
        Assertions.assertEquals(0, apiList.size());
    }
}