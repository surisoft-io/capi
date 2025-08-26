package io.surisoft.capi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.configuration.CapiSslContextHolder;
import io.surisoft.capi.schema.OpaResult;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.SECONDS;

@Component
@ConditionalOnProperty(prefix = "capi.opa", name = "enabled", havingValue = "true")
public class OpaService {

    private static final Logger log = LoggerFactory.getLogger(OpaService.class);
    private final String opaEndpoint;
    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Optional<CapiSslContextHolder> capiSslContextHolder;

    public OpaService(@Value("${capi.opa.endpoint}") String opaEndpoint, Optional<CapiSslContextHolder> capiSslContextHolder) {
        this.opaEndpoint = opaEndpoint;
        this.capiSslContextHolder = capiSslContextHolder;
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        capiSslContextHolder.ifPresent(sslContextHolder -> httpClientBuilder.sslContext(sslContextHolder.getSslContext()));
        httpClientBuilder.connectTimeout(Duration.ofSeconds(10));
        httpClient = httpClientBuilder.build();
    }

    public OpaResult callOpa(String opaRego, String value, boolean isAccessToken) {
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(buildHttpRequest(opaRego, value, isAccessToken), HttpResponse.BodyHandlers.ofString());
            if(httpResponse.statusCode() == 200) {
                return objectMapper.readValue(httpResponse.body(), OpaResult.class);
            }
        } catch (InterruptedException | IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;
    }

    private HttpRequest buildHttpRequest(String opaRego, String value, boolean isAccessToken) throws URISyntaxException {
        return HttpRequest.newBuilder()
                    .uri(new URI(opaEndpoint + "/v1/data/" + opaRego + "/allow"))
                    .setHeader("Media-Type", "application/json")
                    .timeout(Duration.of(10, SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(value, isAccessToken)))
                    .build();
    }

    private String buildRequestBody(String value, boolean isAccessToken) {
        JsonObject tokenObject = new JsonObject();
        if(isAccessToken) {
            tokenObject.put("token", value);
        } else {
            tokenObject.put("consumerKey", value);
        }
        JsonObject inputObject = new JsonObject();
        inputObject.put("input", tokenObject);
        return inputObject.toJson();
    }

    public void reloadHttpClient() {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        capiSslContextHolder.ifPresent(sslContextHolder -> httpClientBuilder.sslContext(sslContextHolder.getSslContext()));
        httpClientBuilder.connectTimeout(Duration.ofSeconds(10));
        httpClient = httpClientBuilder.build();
    }
}