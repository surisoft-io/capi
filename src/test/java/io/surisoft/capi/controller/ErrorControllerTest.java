package io.surisoft.capi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ErrorControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    public void initialize() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testGet() throws Exception {
        // Setup
        // Run the test
        final MockHttpServletResponse response = mockMvc.perform(get("/capi-error/**")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        // Verify the results
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(response.getContentAsString()).isEqualTo("{\"routeID\":null,\"errorMessage\":\"There was an exception connecting to the requested service, please try again later on.\",\"errorCode\":502,\"httpUri\":null,\"traceID\":null}");
    }

    @Test
    void testPost() throws Exception {
        // Setup
        // Run the test
        final MockHttpServletResponse response = mockMvc.perform(post("/capi-error/**")
                        .content("content").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        // Verify the results
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(response.getContentAsString()).isEqualTo("{\"routeID\":null,\"errorMessage\":\"There was an exception connecting to the requested service, please try again later on.\",\"errorCode\":502,\"httpUri\":null,\"traceID\":null}");
    }

    @Test
    void testPut() throws Exception {
        // Setup
        // Run the test
        final MockHttpServletResponse response = mockMvc.perform(put("/capi-error")
                        .content("content").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        // Verify the results
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(response.getContentAsString()).isEqualTo("{\"routeID\":null,\"errorMessage\":\"There was an exception connecting to the requested service, please try again later on.\",\"errorCode\":502,\"httpUri\":null,\"traceID\":null}");
    }

    @Test
    void testDelete() throws Exception {
        // Setup
        // Run the test
        final MockHttpServletResponse response = mockMvc.perform(delete("/capi-error")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();

        // Verify the results
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(response.getContentAsString()).isEqualTo("{\"routeID\":null,\"errorMessage\":\"There was an exception connecting to the requested service, please try again later on.\",\"errorCode\":502,\"httpUri\":null,\"traceID\":null}");
    }
}