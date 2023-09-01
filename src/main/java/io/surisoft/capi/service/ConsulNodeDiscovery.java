package io.surisoft.capi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.builder.DirectRouteProcessor;
import io.surisoft.capi.builder.RestDefinitionProcessor;
import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.utils.ApiUtils;
import io.surisoft.capi.utils.RouteUtils;
import io.surisoft.capi.utils.WebsocketUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.util.json.JsonObject;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class ConsulNodeDiscovery {

    private static final Logger log = LoggerFactory.getLogger(ConsulNodeDiscovery.class);
    private static boolean connectedToConsul = false;
    private String consulHost;
    private final ApiUtils apiUtils;
    private final RouteUtils routeUtils;
    private final MetricsProcessor metricsProcessor;
    private final StickySessionCacheManager stickySessionCacheManager;
    private final HttpClient client;
    private static final String GET_ALL_SERVICES = "/v1/catalog/services";
    private static final String GET_SERVICE_BY_NAME = "/v1/catalog/service/";
    private String capiContext;
    private String reverseProxyHost;
    private final CamelContext camelContext;
    private final Cache<String, Api> apiCache;

    private Map<String, WebsocketClient> websocketClientMap;

    private WebsocketUtils websocketUtils;

    public ConsulNodeDiscovery(CamelContext camelContext, ApiUtils apiUtils, RouteUtils routeUtils, MetricsProcessor metricsProcessor, StickySessionCacheManager stickySessionCacheManager, Cache<String, Api> apiCache, Map<String, WebsocketClient> websocketClientMap) {
        this.apiUtils = apiUtils;
        this.routeUtils = routeUtils;
        this.camelContext = camelContext;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.apiCache = apiCache;
        this.metricsProcessor = metricsProcessor;
        this.websocketClientMap = websocketClientMap;

        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public void processInfo() {
        getAllServices();
    }

    private void getAllServices() {
        log.trace("Querying Consul for new services");
        HttpResponse<String> response;
        try {
            response = client.send(buildServicesHttpRequest(), HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            JsonObject responseObject = objectMapper.readValue(response.body(), JsonObject.class);
            //We want to ignore the consul array
            responseObject.remove("consul");
            Set<String> services = responseObject.keySet();
            try {
                apiUtils.removeUnusedApi(camelContext, routeUtils, apiCache, services.stream().toList());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            for(String service : services) {
                    getServiceByName(service);
            }
            connectedToConsul = true;
        } catch (IOException e) {
            log.error("Error connecting to Consul, will try again...");
        } catch (InterruptedException e) {
            log.error("Error connecting to Consul, will try again...");
            Thread.currentThread().interrupt();

        }
    }

    private void getServiceByName(String serviceName) {
        log.trace("Processing service name: {}", serviceName);
        try {
            HttpResponse<String> response = client.send(buildServiceNameHttpRequest(serviceName), HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            ConsulObject[] consulResponse = objectMapper.readValue(response.body(), ConsulObject[].class);
            Map<String, Set<Mapping>> servicesStructure = groupByServiceId(consulResponse);

            for (var entry : servicesStructure.entrySet()) {
                String apiId = serviceName + ":" + entry.getKey();
                Api incomingApi = createApiObject(apiId, serviceName, entry.getKey(), entry.getValue(), consulResponse);
                Api existingApi = apiCache.peek(apiId);
                if(existingApi == null) {
                    createRoute(incomingApi);
                } else {
                    apiUtils.updateExistingApi(existingApi, incomingApi, apiCache, routeUtils, metricsProcessor, camelContext, stickySessionCacheManager, capiContext, reverseProxyHost);
                }
            }
        } catch (IOException e) {
            log.error("Error connecting to Consul, will try again...");
        } catch (InterruptedException e) {
            log.error("Error connecting to Consul, will try again...");
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Set<Mapping>> groupByServiceId(ConsulObject[] consulService) {
        Map<String, Set<Mapping>> groupedService = new HashMap<>();
        Set<String> serviceIdList = new HashSet<>();
        for(ConsulObject serviceIdEntry : consulService) {
            String serviceNodeGroup = getServiceNodeGroup(serviceIdEntry);
            if(serviceNodeGroup != null) {
                serviceIdList.add(serviceNodeGroup);
            } else {
                log.trace("Meta data {} group not present, service will not be deployed", serviceIdEntry.getServiceName());
            }
        }
        for (String id : serviceIdList) {
            Set<Mapping> mappingList = new HashSet<>();
            for (ConsulObject serviceIdToProcess : consulService) {
                String serviceNodeGroup = getServiceNodeGroup(serviceIdToProcess);
                if (id.equals(serviceNodeGroup)) {
                    Mapping entryMapping = apiUtils.consulObjectToMapping(serviceIdToProcess);
                    mappingList.add(entryMapping);
                }
            }
            groupedService.put(id, mappingList);
        }
        return groupedService;
    }

    private String getServiceNodeGroup(ConsulObject consulObject) {
        if(consulObject.getServiceMeta() != null && consulObject.getServiceMeta().getGroup() != null) {
            return consulObject.getServiceMeta().getGroup();
        }
        return null;
    }

    public HttpProtocol getHttpProtocol(String key, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(Objects.equals(getServiceNodeGroup(entry), key)) {
                if(entry.getServiceMeta().getSchema() == null) {
                    return HttpProtocol.HTTP;
                }
                if(entry.getServiceMeta().getSchema().equals(HttpProtocol.HTTPS.getProtocol())) {
                    return HttpProtocol.HTTPS;
                }
            }
        }
        return HttpProtocol.HTTP;
    }

    public boolean isSecured(String key, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(Objects.equals(getServiceNodeGroup(entry), key)) {
                return entry.getServiceMeta().isSecured();
            }
        }
        return false;
    }

    private boolean showZipkinTraceId(String tagName, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(entry.getServiceMeta() != null) {
                if(entry.getServiceMeta().getGroup() != null && entry.getServiceMeta().getGroup().equals(tagName) && entry.getServiceMeta().isB3TraceId()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isTenantAware(String key, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(Objects.equals(getServiceNodeGroup(entry), key)) {
                return entry.getServiceMeta().isTenantAware();
            }
        }
        return false;
    }

    public boolean isWebsocket(String key, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(Objects.equals(getServiceNodeGroup(entry), key)) {
                return (entry.getServiceMeta().getType() != null && entry.getServiceMeta().getType().equals("websocket"));
            }
        }
        return false;
    }

    public List<String> getSubscriptionGroups(String key, ConsulObject[] consulObject) {
        for(ConsulObject entry : consulObject) {
            if(Objects.equals(getServiceNodeGroup(entry), key)) {
                if(entry.getServiceMeta().getSubscriptionGroup() != null && !entry.getServiceMeta().getSubscriptionGroup().isEmpty()) {
                    return Arrays.asList(entry.getServiceMeta().getSubscriptionGroup().split(",", -1));
                }
            }
        }
        return null;
    }

    private Api createApiObject(String apiId, String serviceName, String key, Set<Mapping> mappingList, ConsulObject[] consulResponse) {
        Api incomingApi = new Api();
        incomingApi.setId(apiId);
        incomingApi.setName(serviceName);
        incomingApi.setContext("/" + serviceName + "/" + key);
        incomingApi.setHttpMethod(HttpMethod.ALL);
        incomingApi.setPublished(true);
        incomingApi.setMatchOnUriPrefix(true);
        incomingApi.setMappingList(mappingList);
        incomingApi.setForwardPrefix(reverseProxyHost != null);
        incomingApi.setZipkinShowTraceId(showZipkinTraceId(key, consulResponse));
        incomingApi.setHttpProtocol(getHttpProtocol(key, consulResponse));
        incomingApi.setSecured(isSecured(key, consulResponse));
        incomingApi.setTenantAware(isTenantAware(key, consulResponse));

        incomingApi.setRoundRobinEnabled(true);
        incomingApi.setFailoverEnabled(true);
        incomingApi.setWebsocket(isWebsocket(key, consulResponse));
        incomingApi.setSubscriptionGroup(getSubscriptionGroups(key, consulResponse));
        incomingApi.setSubscriptionGroup(getSubscriptionGroups(key, consulResponse));

        return incomingApi;
    }

    private void createRoute(Api incomingApi) {
        apiCache.put(incomingApi.getId(), incomingApi);
        if(incomingApi.isWebsocket()) {
            WebsocketClient websocketClient = websocketUtils.createWebsocketClient(incomingApi);
            if(websocketClient != null) {
                websocketClientMap.put(websocketClient.getPath(), websocketClient);
            }
        } else {
            List<String> apiRouteIdList = routeUtils.getAllRouteIdForAGivenApi(incomingApi);
            for(String routeId : apiRouteIdList) {
                Route existingRoute = camelContext.getRoute(routeId);
                if(existingRoute == null) {
                    try {
                        if(incomingApi.getMappingList().size() == 1 || incomingApi.isTenantAware()) {
                            incomingApi.setRoundRobinEnabled(false);
                            incomingApi.setFailoverEnabled(false);
                        }
                        camelContext.addRoutes(new DirectRouteProcessor(camelContext, incomingApi, routeUtils, metricsProcessor, routeId, stickySessionCacheManager, capiContext, reverseProxyHost));
                        camelContext.addRoutes(new RestDefinitionProcessor(camelContext, incomingApi, routeUtils, routeId));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    public void setConsulHost(String consulHost) {
        this.consulHost = consulHost;
    }

    public void setCapiContext(String capiContext) {
        this.capiContext = capiContext;
    }

    private HttpRequest buildServicesHttpRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(consulHost + GET_ALL_SERVICES))
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    private HttpRequest buildServiceNameHttpRequest(String serviceName) {
        return HttpRequest.newBuilder()
                .uri(URI.create(consulHost + GET_SERVICE_BY_NAME + serviceName))
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    public void setReverseProxyHost(String reverseProxyHost) {
        this.reverseProxyHost = reverseProxyHost;
    }

    public static boolean isConnectedToConsul() {
        return connectedToConsul;
    }

    public void setWebsocketUtils(WebsocketUtils websocketUtils) {
        this.websocketUtils = websocketUtils;
    }
}