package io.surisoft.capi.lb.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.cache.RunningApiManager;
import io.surisoft.capi.lb.repository.ApiRepository;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.ConsulObject;
import io.surisoft.capi.lb.schema.HttpMethod;
import io.surisoft.capi.lb.schema.Mapping;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.camel.util.json.JsonObject;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class ConsulNodeDiscovery {

    private String consulHost;
    private ApiUtils apiUtils;
    private ApiRepository apiRepository;
    private RouteUtils routeUtils;
    private RunningApiManager runningApiManager;
    private OkHttpClient client = new OkHttpClient.Builder().build();
    private static final String GET_ALL_SERVICES = "/v1/catalog/services";
    private static final String GET_SERVICE_BY_NAME = "/v1/catalog/service/";

    public ConsulNodeDiscovery(String consulHost, ApiUtils apiUtils, ApiRepository apiRepository, RouteUtils routeUtils, RunningApiManager runningApiManager) {
        this.consulHost = consulHost;
        this.apiUtils = apiUtils;
        this.apiRepository = apiRepository;
        this.routeUtils = routeUtils;
        this.runningApiManager = runningApiManager;
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
                log.info(e.getMessage());
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
                api.setContext("/" + serviceName);
                api.setHttpMethod(HttpMethod.ALL);

                for(ConsulObject consulObject : consulResponse) {
                    Mapping mapping = apiUtils.consulObjectToMapping(consulObject);
                    if(!api.getMappingList().contains(mapping)) {
                        api.getMappingList().add(mapping);
                    }
                }

                Optional<Api> existingApi = apiRepository.findById(apiId);
                if(existingApi.isPresent()) {
                    apiUtils.updateExistingApi(existingApi.get(), api, apiRepository, routeUtils, runningApiManager);
                } else  {
                    apiUtils.applyApiDefaults(api);
                    apiRepository.save(api);
                    if(api.getHttpMethod().equals(HttpMethod.ALL)) {
                        runningApiManager.runApi(routeUtils.getAllRouteIdForAGivenApi(api), api, routeUtils);
                    } else {
                        runningApiManager.runApi(routeUtils.getRouteId(api, api.getHttpMethod().getMethod()), api);
                    }
                    //We need to populate the defaults
                    //Create a new api db and running
                }
            }

            public void onFailure(Call call, IOException e) {
                //TODO
                log.info(e.getMessage());
            }
        });
    }
}
