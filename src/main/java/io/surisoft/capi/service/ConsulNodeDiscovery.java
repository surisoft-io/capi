package io.surisoft.capi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.builder.DirectRouteProcessor;
import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.configuration.CapiSslContextHolder;
import io.surisoft.capi.processor.ContentTypeValidator;
import io.surisoft.capi.processor.ThrottleProcessor;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.utils.*;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.util.json.JsonObject;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
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
    private List<String> consulHostList;
    private final ServiceUtils serviceUtils;
    private final RouteUtils routeUtils;
    private final MetricsProcessor metricsProcessor;
    private StickySessionCacheManager stickySessionCacheManager;
    private HttpClient client;
    private static final String GET_ALL_SERVICES = "/v1/catalog/services";
    private static final String GET_SERVICE_BY_NAME = "/v1/catalog/service/";
    private String capiContext;
    private String reverseProxyHost;
    private final CamelContext camelContext;
    private final Cache<String, Service> serviceCache;
    private final Map<String, WebsocketClient> websocketClientMap;
    private final Map<String, SSEClient> sseClientMap;
    private WebsocketUtils websocketUtils;
    private SSEUtils sseUtils;
    private OpaService opaService;
    private HttpUtils httpUtils;
    private String capiNamespace;
    private boolean strictNamespace;
    private String consulToken;
    private String capiRunningMode;
    private final ContentTypeValidator contentTypeValidator;
    private final ThrottleProcessor throttleProcessor;
    private final CapiSslContextHolder capiSslContextHolder;

    public ConsulNodeDiscovery(CamelContext camelContext,
                               ServiceUtils serviceUtils,
                               RouteUtils routeUtils,
                               MetricsProcessor metricsProcessor,
                               Cache<String, Service> serviceCache,
                               Map<String, WebsocketClient> websocketClientMap,
                               Map<String, SSEClient> sseClientMap,
                               ContentTypeValidator contentTypeValidator,
                               ThrottleProcessor throttleProcessor,
                               CapiSslContextHolder capiSslContextHolder) {
        this.serviceUtils = serviceUtils;
        this.routeUtils = routeUtils;
        this.camelContext = camelContext;
        this.serviceCache = serviceCache;
        this.metricsProcessor = metricsProcessor;
        this.websocketClientMap = websocketClientMap;
        this.sseClientMap = sseClientMap;
        this.contentTypeValidator = contentTypeValidator;
        this.throttleProcessor = throttleProcessor;
        this.capiSslContextHolder = capiSslContextHolder;

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        if(capiSslContextHolder != null) {
            httpClientBuilder.sslContext(capiSslContextHolder.getSslContext());
        }
        httpClientBuilder.connectTimeout(Duration.ofSeconds(10));
        client = httpClientBuilder.build();
    }

    public void processInfo() {
        Map<String, List<ConsulObject>> serviceListObjects = getAllServices();
        lookForRemovedServices(serviceListObjects);
        processServices(serviceListObjects);
    }

    private void lookForRemovedServices(Map<String, List<ConsulObject>> serviceListObjects) {
        Map<String, ConsulObject> servicesOnConsul = new HashMap<>();
        for(Map.Entry<String, List<ConsulObject>> entry : serviceListObjects.entrySet()) {
            List<ConsulObject> serviceList = entry.getValue();
            for(ConsulObject consulObject : serviceList) {
                String serviceId = null;
                if(consulObject.getServiceMeta().isRouteGroupFirst()) {
                    serviceId = consulObject.getServiceMeta().getGroup() + ":" + consulObject.getServiceName();
                } else {
                    serviceId = consulObject.getServiceName() + ":" + consulObject.getServiceMeta().getGroup();
                }
                servicesOnConsul.put(serviceId, consulObject);
            }
        }
        try {
            for(CacheEntry<String, Service> stringServiceCacheEntry : serviceCache.entries()) {
                if(!servicesOnConsul.containsKey(stringServiceCacheEntry.getKey())) {
                    log.debug("This service is not on Consul: {}, and will be removed.", stringServiceCacheEntry.getKey());
                    serviceUtils.removeUnusedService(camelContext, routeUtils, Objects.requireNonNull(serviceCache.get(stringServiceCacheEntry.getKey())));
                    serviceCache.remove(stringServiceCacheEntry.getKey());
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }
    }

    private Map<String, List<ConsulObject>> getAllServices() {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, List<ConsulObject>> serviceListObjects = new HashMap<>();
        HttpResponse<String> response;
        try {
            for(String consulHost : consulHostList) {
                log.trace("Querying Consul {} for new services", consulHost);
                response = client.send(buildServicesHttpRequest(consulHost), HttpResponse.BodyHandlers.ofString());
                JsonObject responseObject = objectMapper.readValue(response.body(), JsonObject.class);
                //We want to ignore the consul array
                responseObject.remove("consul");
                Set<String> services = responseObject.keySet();
                for(String serviceName : services) {
                    List<ConsulObject> consulInstanceObjectList = getServiceByName(consulHost, serviceName);
                    if(consulInstanceObjectList != null) {
                        if(serviceListObjects.containsKey(serviceName)) {
                            serviceListObjects.get(serviceName).addAll(consulInstanceObjectList);
                        } else {
                            serviceListObjects.put(serviceName, consulInstanceObjectList);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(ErrorMessage.ERROR_CONNECTING_TO_CONSUL);
        } catch (InterruptedException e) {
            log.error(ErrorMessage.ERROR_CONNECTING_TO_CONSUL);
            Thread.currentThread().interrupt();

        }
        return serviceListObjects;
    }

    private List<ConsulObject> getServiceByName(String consulHost, String serviceName) {
        log.trace("Getting service name: {} at consul host: {}", serviceName, consulHost);
        List<ConsulObject> servicesToDeploy = new ArrayList<>();
        try {
            HttpResponse<String> response = client.send(buildServiceNameHttpRequest(consulHost, serviceName), HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<List<ConsulObject>> typeRef = new TypeReference<>() {};
            List<ConsulObject> temporaryList = objectMapper.readValue(response.body(), typeRef);
            temporaryList.forEach(o -> {
                if(capiNamespace == null) {
                    servicesToDeploy.add(o);
                } else {
                    if(o.getServiceMeta().getNamespace() == null) {
                        if(!strictNamespace) {
                            servicesToDeploy.add(o);
                        }
                    } else if(o.getServiceMeta().getNamespace().equals(capiNamespace)) {
                        servicesToDeploy.add(o);
                    }
                }
            });
            return servicesToDeploy;
        } catch (IOException e) {
            log.error(ErrorMessage.ERROR_CONNECTING_TO_CONSUL);
        } catch (InterruptedException e) {
            log.error(ErrorMessage.ERROR_CONNECTING_TO_CONSUL);
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private void processServices(Map<String, List<ConsulObject>> serviceListObjects) {
        serviceListObjects.forEach((serviceName, objectList) -> {
            log.trace("Processing service name: {}", serviceName);
            Map<String, Set<Mapping>> servicesStructure = groupByServiceId(objectList);
            for (var entry : servicesStructure.entrySet()) {
                Service incomingService = createServiceObject(serviceName, entry.getKey(), entry.getValue(), objectList);
                Service existingService = serviceCache.peek(incomingService.getId());
                if(existingService == null) {
                    if(serviceUtils.checkIfOpenApiIsEnabled(incomingService, client)) {
                        createRoute(incomingService);
                    }
                } else {
                    if(serviceUtils.updateExistingService(existingService, incomingService, serviceCache)) {
                        if(serviceUtils.checkIfOpenApiIsEnabled(incomingService, client)) {
                            createRoute(incomingService);
                        }
                    }
                }
            }
        });
        connectedToConsul = true;
    }

    private Map<String, Set<Mapping>> groupByServiceId(List<ConsulObject> consulService) {
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
                    Mapping entryMapping = serviceUtils.consulObjectToMapping(serviceIdToProcess);
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

    public ServiceMeta getServiceMeta(String key, List<ConsulObject> consulObject) {
        for(ConsulObject entry : consulObject) {
            if(Objects.equals(getServiceNodeGroup(entry), key)) {
                return entry.getServiceMeta();
            }
        }
        return null;
    }

    public int getModifyIndex(String key, List<ConsulObject> consulObject) {
        for(ConsulObject entry : consulObject) {
            if(Objects.equals(getServiceNodeGroup(entry), key)) {
                return entry.getModifyIndex();
            }
        }
        return -1;
    }

    private Service createServiceObject(String serviceName, String key, Set<Mapping> mappingList, List<ConsulObject> consulResponse) {
        Service incomingService = new Service();
        ServiceMeta serviceMeta = getServiceMeta(key, consulResponse);

        if(serviceMeta.isRouteGroupFirst()) {
            incomingService.setId(key + ":" + serviceName);
            incomingService.setContext("/" + key + "/" + serviceName);
        } else {
            incomingService.setId(serviceName + ":" + key);
            incomingService.setContext("/" + serviceName + "/" + key);
        }

        incomingService.setName(serviceName);
        incomingService.setRegisteredBy(getClass().getName());

        incomingService.setMappingList(mappingList);
        incomingService.setServiceMeta(getServiceMeta(key, consulResponse));
        //incomingService.setServiceIdConsul(getServiceIdConsul(key, consulResponse));
        incomingService.setRoundRobinEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware() && !incomingService.getServiceMeta().isStickySession());
        incomingService.setFailOverEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware() && !incomingService.getServiceMeta().isStickySession());

        incomingService.setModifyIndex(getModifyIndex(key, consulResponse));

        serviceUtils.validateServiceType(incomingService);
        return incomingService;
    }

    private void createRoute(Service incomingService) {
        if(incomingService.getServiceMeta().getState() == null || incomingService.getServiceMeta().getState().equals(State.PUBLISHED)) {
            serviceCache.put(incomingService.getId(), incomingService);
            if(incomingService.getServiceMeta().getType().equalsIgnoreCase(Constants.WEBSOCKET_TYPE) &&
                    (capiRunningMode.equalsIgnoreCase(Constants.WEBSOCKET_TYPE) || capiRunningMode.equalsIgnoreCase(Constants.FULL_TYPE)) && websocketUtils != null) {
                WebsocketClient websocketClient = websocketUtils.createWebsocketClient(incomingService);
                if(websocketClient != null && websocketClientMap != null) {
                    websocketClientMap.put(websocketClient.getServiceId(), websocketClient);
                }
            } else if(incomingService.getServiceMeta().getType().equalsIgnoreCase(Constants.SSE_TYPE) &&
                    (capiRunningMode.equalsIgnoreCase(Constants.SSE_TYPE) || capiRunningMode.equalsIgnoreCase(Constants.FULL_TYPE))) {
                log.trace("Creating SSE client for service: {}", incomingService.getId());
                SSEClient sseClient = sseUtils.createSSEClient(incomingService);
                if(sseClient != null && sseClientMap != null) {
                    sseClientMap.put(sseClient.getApiId(), sseClient);
                }

            } else if(capiRunningMode.equalsIgnoreCase(Constants.FULL_TYPE) && (incomingService.getServiceMeta().getType() == null || incomingService.getServiceMeta().getType().equals("rest"))) {
                List<String> apiRouteIdList = routeUtils.getAllRouteIdForAGivenService(incomingService);
                for(String routeId : apiRouteIdList) {
                    Route existingRoute = camelContext.getRoute(routeId);
                    if(existingRoute == null) {
                        try {
                            DirectRouteProcessor directRouteProcessor = new DirectRouteProcessor(camelContext, incomingService, routeUtils, metricsProcessor, routeId, capiContext, reverseProxyHost, contentTypeValidator, throttleProcessor);
                            directRouteProcessor.setHttpUtils(httpUtils);
                            directRouteProcessor.setOpaService(opaService);
                            directRouteProcessor.setStickySessionCacheManager(stickySessionCacheManager);
                            directRouteProcessor.setServiceCache(serviceCache);
                            camelContext.addRoutes(directRouteProcessor);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    public void setConsulHostList(List<String> consulHostList) {
        this.consulHostList = consulHostList;
    }

    public void setCapiContext(String capiContext) {
        this.capiContext = capiContext;
    }

    private HttpRequest buildServicesHttpRequest(String consulHost) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        URI uri = URI.create(consulHost + GET_ALL_SERVICES);
        if (uri.getPath() != null && uri.getPath().contains("..")) {
            throw new IllegalArgumentException("Path traversal detected in URI path: " + uri.getPath());
        }
        if(consulToken != null) {
            builder.header(Constants.AUTHORIZATION_HEADER, Constants.BEARER + consulToken);
        }
        return builder
                .uri(uri)
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    private HttpRequest buildServiceNameHttpRequest(String consulHost, String serviceName) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        URI uri = URI.create(consulHost + GET_SERVICE_BY_NAME + serviceName);
        if (uri.getPath() != null && uri.getPath().contains("..")) {
            throw new IllegalArgumentException("Path traversal detected in URI path: " + uri.getPath());
        }
        if(consulToken != null) {
            builder.header(Constants.AUTHORIZATION_HEADER, Constants.BEARER + consulToken.replaceAll("(\r\n|\n)", ""));
        }
        return builder
                .uri(uri)
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

    public void setSSEUtils(SSEUtils sseUtils) {
        this.sseUtils = sseUtils;
    }

    public void setStickySessionCacheManager(StickySessionCacheManager stickySessionCacheManager) {
        this.stickySessionCacheManager = stickySessionCacheManager;
    }

    public void setOpaService(OpaService opaService) {
        this.opaService = opaService;
    }

    public void setHttpUtils(HttpUtils httpUtils) {
        this.httpUtils = httpUtils;
    }

    public void setCapiNamespace(String capiNamespace) {
        this.capiNamespace = capiNamespace;
    }
    public void setStrictNamespace(boolean strictNamespace) {
        this.strictNamespace = strictNamespace;
    }

    public void setConsulToken(String consulToken) {
        this.consulToken = consulToken;
    }

    public void setCapiRunningMode(String capiRunningMode) {
        this.capiRunningMode = capiRunningMode;
    }

    public void reloadHttpClient() {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        if(capiSslContextHolder != null) {
            httpClientBuilder.sslContext(capiSslContextHolder.getSslContext());
        }
        httpClientBuilder.connectTimeout(Duration.ofSeconds(10));
        client = httpClientBuilder.build();
    }
}