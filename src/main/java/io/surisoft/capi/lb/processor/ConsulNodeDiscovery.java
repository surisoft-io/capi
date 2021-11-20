package io.surisoft.capi.lb.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.ConsulObject;
import io.surisoft.capi.lb.utils.ApiUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.camel.util.json.JsonObject;

import java.io.IOException;
import java.util.Set;

@Slf4j
public class ConsulNodeDiscovery {

    private String consulHost;
    private ApiUtils apiUtils;
    private OkHttpClient client = new OkHttpClient.Builder().build();
    private static final String GET_ALL_SERVICES = "/v1/catalog/services";
    private static final String GET_SERVICE_BY_NAME = "/v1/catalog/service/";

    public ConsulNodeDiscovery(String consulHost, ApiUtils apiUtils) {
        this.consulHost = consulHost;
        this.apiUtils = apiUtils;
    }

    public void processInfo() {
        getAllServices();
    }

    private void getAllServices() {
        Request request = new Request.Builder().url(consulHost + GET_ALL_SERVICES).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonObject responseObject = objectMapper.readValue(response.body().string(), JsonObject.class);
                //We want to ignore the consul array for now...
                responseObject.remove("consul");
                Set<String> services = responseObject.keySet();
                for(String service : services) {
                    getServiceByName(service);
                }
            }

            public void onFailure(Call call, IOException e) {
                //TODO
            }
        });
    }

    private void getServiceByName(String serviceName) {
        Request request = new Request.Builder().url(consulHost + GET_SERVICE_BY_NAME + serviceName).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {
                ObjectMapper objectMapper = new ObjectMapper();
                ConsulObject[] consulResponse = objectMapper.readValue(response.body().string(), ConsulObject[].class);
                String apiId = apiUtils.getApiId(serviceName);
                log.info(apiId);
                Api api = new Api();
                api.setId(apiId);
                api.setName(serviceName);

                for(ConsulObject consulObject : consulResponse) {
                    api.getMappingList().add(apiUtils.consulObjectToMapping(consulObject));
                }
            }

            public void onFailure(Call call, IOException e) {
                //TODO
            }
        });
    }
}
