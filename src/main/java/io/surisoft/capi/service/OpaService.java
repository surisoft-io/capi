package io.surisoft.capi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.schema.OpaResult;
import okhttp3.*;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "capi.opa", name = "enabled", havingValue = "true")
public class OpaService {

    private static final Logger log = LoggerFactory.getLogger(OpaService.class);
    private final OkHttpClient httpClient;
    @Value("${capi.opa.endpoint}")
    private String opaEndpoint;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OpaService(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    public OpaResult callOpa(String opaRego, String value, boolean isAccessToken) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (Response opaResponse = httpClient.newCall(buildHttpRequest(opaRego, value, isAccessToken)).execute()) {
            assert opaResponse.body() != null;
            return objectMapper.readValue(opaResponse.body().string(), OpaResult.class);
        }
    }

    private Request buildHttpRequest(String opaRego, String value, boolean isAccessToken) {
        return new Request.Builder()
                .url(opaEndpoint + "/v1/data/" + opaRego + "/allow")
                .post(buildRequestBody(value, isAccessToken))
                .build();
    }

    private RequestBody buildRequestBody(String value, boolean isAccessToken) {
        JsonObject tokenObject = new JsonObject();
        if(isAccessToken) {
            tokenObject.put("token", value);
        } else {
            tokenObject.put("consumerKey", value);
        }
        JsonObject inputObject = new JsonObject();
        inputObject.put("input", tokenObject);
        return RequestBody.create(inputObject.toJson(), JSON);
    }
}
