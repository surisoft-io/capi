package io.surisoft.capi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/definitions/openapi")
public class DefinitionController {

    private static final Logger log = LoggerFactory.getLogger(DefinitionController.class);
    private final Cache<String, Service> serviceCache;
    private final String capiPublicEndpoint;
    private final HttpUtils httpUtils;

    public DefinitionController(Cache<String, Service> serviceCache,
                                @Value("${capi.public-endpoint}") String capiPublicEndpoint,
                                HttpUtils httpUtils) {
        this.serviceCache = serviceCache;
        this.capiPublicEndpoint = capiPublicEndpoint;
        this.httpUtils = httpUtils;
    }

    @GetMapping(path= "/{serviceId}", produces="application/json")
    public ResponseEntity<JsonObject> getServiceOpenApi(@PathVariable String serviceId, HttpServletRequest request) {
        if(serviceCache.containsKey(serviceId)) {
            try {
                Service service = serviceCache.get(serviceId);
                if(service != null && service.getServiceMeta() != null && service.getServiceMeta().getOpenApiEndpoint() != null) {
                    if(service.getServiceMeta().isExposeOpenApiDefinition()) {
                        if(service.getServiceMeta().isSecureOpenApiDefinition()) {
                            String accessToken = httpUtils.processAuthorizationAccessToken(request);
                            if(accessToken != null) {
                                if(httpUtils.isAuthorized(accessToken, service.getServiceMeta().getSubscriptionGroup())) {
                                    JsonObject responseObject = getDefinition(service);
                                    return new ResponseEntity<>(responseObject, HttpStatus.OK);
                                }
                            }
                        } else {
                            JsonObject responseObject = getDefinition(service);
                            return new ResponseEntity<>(responseObject, HttpStatus.OK);
                        }
                    }
                }
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (NullPointerException | IOException | InterruptedException | AuthorizationException e) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private JsonObject getDefinition(Service service) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(service.getServiceMeta().getOpenApiEndpoint())).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject serverObject = new JsonObject();
        serverObject.put("url", capiPublicEndpoint + service.getId().replaceAll(":", "/"));
        JsonObject responseObject = objectMapper.readValue(response.body(), JsonObject.class);
        responseObject.remove("servers");

        JsonArray serversArray = new JsonArray();
        serversArray.add(serverObject);

        JsonObject infoObject = new JsonObject();
        infoObject.put("title", service.getId());
        infoObject.put("description", "Open API definition generated by CAPI");
        responseObject.put("info", infoObject);
        responseObject.put("servers", serversArray);
        return responseObject;
    }
}
