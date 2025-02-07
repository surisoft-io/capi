package io.surisoft.capi.utils;

import io.surisoft.capi.schema.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class ServiceUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);
    private final HttpUtils httpUtils;
    private final Optional<Map<String, WebsocketClient>> websocketClientMap;
    private final Optional<Map<String, SSEClient>> sseClientMap;
    private final RouteUtils routeUtils;
    private final CamelContext camelContext;
    private final Optional<WebsocketUtils> websocketUtils;
    private final String capiRunningMode;

    public ServiceUtils(HttpUtils httpUtils,
                        Optional<Map<String, WebsocketClient>> websocketClientMap,
                        Optional<Map<String, SSEClient>> sseClientMap,
                        RouteUtils routeUtils,
                        CamelContext camelContext,
                        Optional<WebsocketUtils> websocketUtils,
                        @Value("${capi.mode}") String capiRunningMode) {
        this.httpUtils = httpUtils;
        this.websocketClientMap = websocketClientMap;
        this.sseClientMap = sseClientMap;
        this.routeUtils = routeUtils;
        this.camelContext = camelContext;
        this.websocketUtils = websocketUtils;
        this.capiRunningMode = capiRunningMode;
    }

    public String getServiceId(Service service) {
        return service.getName() + ":" + service.getServiceMeta().getGroup();
    }

    public Mapping consulObjectToMapping(ConsulObject consulObject) {
        String host = consulObject.getServiceAddress();
        int port = consulObject.getServicePort();
        Mapping mapping = new Mapping();

        if(consulObject.getServiceMeta() != null && consulObject.getServiceMeta().getIngress() != null) {
            mapping.setHostname(httpUtils.normalizeHttpEndpoint(consulObject.getServiceMeta().getIngress()));
            mapping.setIngress(true);
            if(httpUtils.isEndpointSecure(consulObject.getServiceMeta().getIngress())) {
                mapping.setPort(Constants.HTTPS_PORT);
            } else {
                mapping.setPort(Constants.HTTP_PORT);
            }
        } else {
            mapping.setHostname(host);
            mapping.setPort(port);
        }

        if(consulObject.getServiceMeta().getRootContext() != null && !consulObject.getServiceMeta().getRootContext().isEmpty()) {
            if(consulObject.getServiceMeta().getRootContext().startsWith("/")) {
                mapping.setRootContext(consulObject.getServiceMeta().getRootContext());
            } else {
                mapping.setRootContext("/" + consulObject.getServiceMeta().getRootContext());
            }
        } else {
            mapping.setRootContext("/");
        }
        mapping.setTenandId(consulObject.getServiceMeta().getTenantId() != null ? consulObject.getServiceMeta().getTenantId() : null);
        return mapping;
    }

    public void validateServiceType(Service service) {
        if(service.getServiceMeta().getType() == null) {
            service.getServiceMeta().setType("rest");
        }
    }

    public boolean updateExistingService(Service existingService,
                                      Service incomingService,
                                      Cache<String, Service> serviceCache) {

        if(!Objects.equals(existingService.getServiceIdConsul(), incomingService.getServiceIdConsul())) {
            redeployService(incomingService, existingService, serviceCache);
            serviceCache.remove(existingService.getId());
            return true;
        } else if(isMappingChanged(existingService.getMappingList().stream().toList(), incomingService.getMappingList().stream().toList())) {
            redeployService(incomingService, existingService, serviceCache);
            serviceCache.remove(existingService.getId());
            return true;
        } else {
            log.trace("No changes detected for Service: {}.", existingService.getId());
            return false;
        }
    }

    private void redeployService(Service incomingService, Service existingService, Cache<String, Service> serviceCache) {
        log.trace("Changes detected for Service: {}, redeploying routes.", existingService.getId());
        if(existingService.getServiceMeta().getType() != null &&
                existingService.getServiceMeta().getType().equals(Constants.WEBSOCKET_TYPE) &&
                incomingService.getServiceMeta().getType().equals(Constants.WEBSOCKET_TYPE) &&
                websocketClientMap.isPresent() &&
                websocketClientMap.get().containsKey(existingService.getId())) {
            websocketClientMap.get().remove(existingService.getContext());
        } else if(existingService.getServiceMeta().getType() != null &&
                existingService.getServiceMeta().getType().equals(Constants.SSE_TYPE) &&
                incomingService.getServiceMeta().getType().equals(Constants.SSE_TYPE) &&
                sseClientMap.isPresent() &&
                sseClientMap.get().containsKey(existingService.getId())) {
            sseClientMap.get().remove(existingService.getId());
        } else {
            try {
                List<String> apiRouteIdList = routeUtils.getAllRouteIdForAGivenService(existingService);
                for(String routeId : apiRouteIdList) {
                    camelContext.getRouteController().stopRoute(routeId);
                    camelContext.removeRoute(routeId);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public boolean isMappingChanged(List<Mapping> existingMappingList, List<Mapping> incomingMappingList) {
        if(existingMappingList.size() != incomingMappingList.size()) {
            return true;
        }
        for(Mapping incomingMapping : incomingMappingList) {
            if(!existingMappingList.contains(incomingMapping)) {
                return true;
            }
        }
        return false;
    }


    public void removeUnusedService(CamelContext camelContext, RouteUtils routeUtils, Service service) throws Exception {
        if(service.getServiceMeta().getType().equals("websocket") && websocketClientMap.isPresent() && websocketUtils.isPresent()) {
            websocketUtils.get().removeClientFromMap(websocketClientMap.get(), service);
        } else if(service.getServiceMeta().getType().equals("sse") && sseClientMap.isPresent()) {
            sseClientMap.get().remove(service.getContext());
        } else {
            List<String> serviceRouteIdList = routeUtils.getAllRouteIdForAGivenService(service);
            for (String routeId : serviceRouteIdList) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
            }
        }
    }

    public boolean checkIfOpenApiIsEnabled(Service service, HttpClient httpClient) {
        if(capiRunningMode.equalsIgnoreCase(Constants.FULL_TYPE) && service.getServiceMeta() != null && service.getServiceMeta().getOpenApiEndpoint() != null && !service.getServiceMeta().getOpenApiEndpoint().isEmpty()) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder();
                URI uri = URI.create(service.getServiceMeta().getOpenApiEndpoint());
                if (uri.getPath() != null && uri.getPath().contains("..")) {
                    throw new IllegalArgumentException("Path traversal detected in URI path: " + uri.getPath());
                }
                HttpRequest request =  builder
                        .uri(uri)
                        .timeout(Duration.ofMinutes(2))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.trace("Calling Remote Open API Spec: {}", service.getServiceMeta().getOpenApiEndpoint());
                if(response.statusCode() == 200) {
                    assert response.body() != null;
                    OpenAPI openAPI = new OpenAPIV3Parser().readContents(response.body()).getOpenAPI();
                    service.setOpenAPI(openAPI);
                    return true;
                } else {
                    log.warn("Open API specification is invalid for service {}, response code: {}", service.getId(), response.statusCode());
                    return false;
                }
            } catch(Exception e) {
                log.warn(e.getMessage(), e);
                log.warn("Open API specification is invalid for service {}", service.getId());
                return false;
            }
        } else {
            return true;
        }
    }
}