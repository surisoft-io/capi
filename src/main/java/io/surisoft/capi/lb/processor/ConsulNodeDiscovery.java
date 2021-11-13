package io.surisoft.capi.lb.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.schema.ConsulObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

@Slf4j
public class ConsulNodeDiscovery {

    private String consulHost;
    private OkHttpClient client = new OkHttpClient.Builder().build();
    private static final String GET_ALL_SERVICES = "/v1/catalog/services";
    private static final String GET_SERVICE_BY_NAME = "/v1/catalog/service/";

    public ConsulNodeDiscovery(String consulHost) {
        this.consulHost = consulHost;
    }

    //"/v1/catalog/service/service-1"

    public void processInfo() {

        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url(consulHost + "/v1/catalog/service/service-1")
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {

                ObjectMapper objectMapper = new ObjectMapper();
                ConsulObject[] consulResponse = objectMapper.readValue(response.body().string(), ConsulObject[].class);
                log.info(consulResponse[0].getServiceName());
            }

            public void onFailure(Call call, IOException e) {
                //ttttt
            }
        });




    }

    /*private Response getAllServices() {
        Request request = new Request.Builder().url(consulHost + GET_ALL_SERVICES).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {
                ObjectMapper objectMapper = new ObjectMapper();
                ConsulObject[] consulResponse = objectMapper.readValue(response.body().string(), ConsulObject[].class);
                log.info(consulResponse[0].getServiceName());
            }

            public void onFailure(Call call, IOException e) {
                //ttttt
            }
        });
    }*/
}
