package io.surisoft.capi.lb.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.cache.ConsulCacheManager;
import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.configuration.ConsulDirectRouteProcessor;
import io.surisoft.capi.lb.configuration.ConsulRestDefinitionProcessor;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.ConsulObject;
import io.surisoft.capi.lb.schema.HttpMethod;
import io.surisoft.capi.lb.schema.Mapping;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.RouteUtils;
import okhttp3.*;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ConsulNodeDiscovery {

    private static final Logger log = LoggerFactory.getLogger(ConsulNodeDiscovery.class);

    private final String consulHost;
    private final ApiUtils apiUtils;
    private final RouteUtils routeUtils;
    private final MetricsProcessor metricsProcessor;
    private final StickySessionCacheManager stickySessionCacheManager;
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private static final String GET_ALL_SERVICES = "/v1/catalog/services";
    private static final String GET_SERVICE_BY_NAME = "/v1/catalog/service/";
    private final String capiContext;

    private final CamelContext camelContext;
    private final ConsulCacheManager consulCacheManager;

    public ConsulNodeDiscovery(CamelContext camelContext, String consulHost, ApiUtils apiUtils, RouteUtils routeUtils, MetricsProcessor metricsProcessor, StickySessionCacheManager stickySessionCacheManager, ConsulCacheManager consulCacheManager, String capiContext) {
        this.consulHost = consulHost;
        this.apiUtils = apiUtils;
        this.routeUtils = routeUtils;
        this.camelContext = camelContext;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.consulCacheManager = consulCacheManager;
        this.capiContext = capiContext;
        this.metricsProcessor = metricsProcessor;
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
                try {
                    apiUtils.removeConsulUnusedApi(camelContext, routeUtils, consulCacheManager, services.stream().toList());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
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
                Map<String, List<Mapping>> servicesStructure = groupByServiceId(consulResponse);

                for (var entry : servicesStructure.entrySet()) {
                    String apiId = serviceName + ":" + entry.getKey();
                    Api incomingApi = createApiObject(apiId, serviceName, entry.getKey(), entry.getValue(), consulResponse);

                    Api existingApi = consulCacheManager.getApiById(apiId);
                    if(existingApi == null) {
                        createRoute(incomingApi);
                    } else {
                        apiUtils.updateConsulExistingApi(existingApi,incomingApi, consulCacheManager, routeUtils, metricsProcessor, camelContext, stickySessionCacheManager, capiContext);
                    }
                }

                try {
                    apiUtils.removeConsulUnusedApi(camelContext, routeUtils, consulCacheManager, servicesStructure, serviceName);
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            public void onFailure(Call call, IOException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    private Map<String, List<Mapping>> groupByServiceId(ConsulObject[] consulService) {
        Map<String, List<Mapping>> groupedService = new HashMap<>();
        Set<String> serviceIdList = new HashSet<>();
        for(ConsulObject serviceIdEntry : consulService) {
            String serviceNodeGroup = getServiceNodeGroup(serviceIdEntry);
            if(serviceNodeGroup != null) {
                serviceIdList.add(serviceNodeGroup);
            } else {
                log.trace("Service Tag group not present, service will not be deployed");
            }
        }
        Iterator<String> iterator = serviceIdList.iterator();
        while(iterator.hasNext()) {
            String id = iterator.next();
            List<Mapping> mappingList = new ArrayList<>();
            for(ConsulObject serviceIdToProcess : consulService) {
                String serviceNodeGroup = getServiceNodeGroup(serviceIdToProcess);
                if(id.equals(serviceNodeGroup)) {
                    Mapping entryMapping = apiUtils.consulObjectToMapping(serviceIdToProcess);
                    mappingList.add(entryMapping);
                }
            }
            groupedService.put(id, mappingList);
        }
        return groupedService;
    }

    private String getServiceNodeGroup(ConsulObject consulObject) {
        for(String serviceTag : consulObject.getServiceTags()) {
            if(serviceTag.contains(Constants.CONSUL_GROUP)) {
                return serviceTag.substring(serviceTag.lastIndexOf("=") + 1);
            }
        }
        return null;
    }

    private boolean forwardPrefix(String tagName, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(entry.getServiceTags().contains(Constants.CONSUL_GROUP + tagName) && entry.getServiceTags().contains(Constants.X_FORWARDED_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private boolean showZipkinTraceId(String tagName, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(entry.getServiceTags().contains(Constants.CONSUL_GROUP + tagName) && entry.getServiceTags().contains(Constants.TRACE_ID_HEADER)) {
                return true;
            }
        }
        return false;
    }

    private boolean showZipkinServiceName(String tagName, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(entry.getServiceTags().contains(Constants.CONSUL_GROUP + tagName) && entry.getServiceTags().contains(Constants.TRACE_ID_HEADER)) {
                return true;
            }
        }
        return false;
    }

    private Api createApiObject(String apiId, String serviceName, String key, List<Mapping> mappingList, ConsulObject[] consulResponse) {
        Api incomingApi = new Api();
        incomingApi.setId(apiId);
        incomingApi.setName(serviceName);
        incomingApi.setContext("/" + serviceName + "/" + key);
        incomingApi.setHttpMethod(HttpMethod.ALL);
        incomingApi.setPublished(true);
        incomingApi.setMappingList(new ArrayList<>());
        incomingApi.setFailoverEnabled(true);
        incomingApi.setRoundRobinEnabled(true);
        incomingApi.setMatchOnUriPrefix(true);
        incomingApi.setMappingList(mappingList);
        incomingApi.setForwardPrefix(forwardPrefix(key, consulResponse));
        incomingApi.setZipkinShowTraceId(showZipkinTraceId(key, consulResponse));
        return incomingApi;
    }

    private void createRoute(Api incomingApi) {
        consulCacheManager.createApi(incomingApi);
        List<String> apiRouteIdList = routeUtils.getAllRouteIdForAGivenApi(incomingApi);
        for(String routeId : apiRouteIdList) {
            Route existingRoute = camelContext.getRoute(routeId);
            if(existingRoute == null) {
                try {
                    camelContext.addRoutes(new ConsulRestDefinitionProcessor(camelContext, incomingApi, routeUtils, routeId));
                    camelContext.addRoutes(new ConsulDirectRouteProcessor(camelContext, incomingApi, routeUtils, metricsProcessor, routeId, stickySessionCacheManager, capiContext));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}