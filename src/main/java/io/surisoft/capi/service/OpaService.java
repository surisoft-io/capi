package io.surisoft.capi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.schema.OpaResult;
import okhttp3.*;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "opa", name = "enabled", havingValue = "true")
public class OpaService {

    private static final Logger log = LoggerFactory.getLogger(OpaService.class);

    @Autowired
    private OkHttpClient httpClient;

    @Value("${opa.endpoint}")
    private String opaEndpoint;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OpaResult callOpa(String opaRego, String accessToken) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (Response opaResponse = httpClient.newCall(buildHttpRequest(opaRego, accessToken)).execute()) {
            assert opaResponse.body() != null;
            return objectMapper.readValue(opaResponse.body().string(), OpaResult.class);
        }
    }

    private Request buildHttpRequest(String opaRego, String accessToken) {
        return new Request.Builder()
                .url(opaEndpoint + "/v1/data/" + opaRego + "/allow")
                .post(buildRequestBody(accessToken))
                .build();
    }

    private RequestBody buildRequestBody(String accessToken) {
        JsonObject tokenObject = new JsonObject();
        tokenObject.put("token", accessToken);
        JsonObject inputObject = new JsonObject();
        inputObject.put("input", tokenObject);
        return RequestBody.create(inputObject.toJson(), JSON);
    }
}
