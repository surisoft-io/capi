package io.surisoft.capi.controller;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ServiceMeta;
import io.surisoft.capi.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.util.json.JsonObject;
import org.cache2k.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefinitionControllerTest {

    @Mock
    private Cache<String, Service> serviceCache;
    @Mock
    private HttpUtils httpUtils;
    @Mock
    private HttpServletRequest request;

    private DefinitionController controllerUnderTest;

    @BeforeEach
    void setUp() {
        controllerUnderTest = new DefinitionController(serviceCache, "https://api.example.com", httpUtils);
    }

    @Test
    void serviceNotInCache_returns404() {
        when(serviceCache.containsKey("unknown-service")).thenReturn(false);

        ResponseEntity<JsonObject> response = controllerUnderTest.getServiceOpenApi("unknown-service", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void serviceInCacheDefinitionNotExposed_returns404() {
        Service service = new Service();
        ServiceMeta meta = new ServiceMeta();
        meta.setOpenApiEndpoint("http://backend/v3/api-docs");
        meta.setExposeOpenApiDefinition(false);
        service.setServiceMeta(meta);

        when(serviceCache.containsKey("my-service")).thenReturn(true);
        when(serviceCache.get("my-service")).thenReturn(service);

        ResponseEntity<JsonObject> response = controllerUnderTest.getServiceOpenApi("my-service", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void secureDefinitionNoAuthToken_returns404() throws Exception {
        Service service = new Service();
        ServiceMeta meta = new ServiceMeta();
        meta.setOpenApiEndpoint("http://backend/v3/api-docs");
        meta.setExposeOpenApiDefinition(true);
        meta.setSecureOpenApiDefinition(true);
        service.setServiceMeta(meta);

        when(serviceCache.containsKey("my-service")).thenReturn(true);
        when(serviceCache.get("my-service")).thenReturn(service);
        when(httpUtils.processAuthorizationAccessToken(request)).thenReturn(null);

        ResponseEntity<JsonObject> response = controllerUnderTest.getServiceOpenApi("my-service", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void secureDefinitionUnauthorized_returns404() throws Exception {
        Service service = new Service();
        ServiceMeta meta = new ServiceMeta();
        meta.setOpenApiEndpoint("http://backend/v3/api-docs");
        meta.setExposeOpenApiDefinition(true);
        meta.setSecureOpenApiDefinition(true);
        meta.setSubscriptionGroup("premium");
        service.setServiceMeta(meta);

        when(serviceCache.containsKey("my-service")).thenReturn(true);
        when(serviceCache.get("my-service")).thenReturn(service);
        when(httpUtils.processAuthorizationAccessToken(request)).thenReturn("some-token");
        when(httpUtils.isAuthorized("some-token", "premium")).thenReturn(false);

        ResponseEntity<JsonObject> response = controllerUnderTest.getServiceOpenApi("my-service", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
