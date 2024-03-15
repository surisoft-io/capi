package io.surisoft.capi.utils;

import io.surisoft.capi.builder.DirectRouteProcessor;
import io.surisoft.capi.builder.RestDefinitionProcessor;
import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.ConsulObject;
import io.surisoft.capi.schema.Mapping;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.OpaService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class ServiceUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);
    private final HttpUtils httpUtils;
    private final Optional<Map<String, WebsocketClient>> websocketClientMap;
    private final RouteUtils routeUtils;
    private final MetricsProcessor metricsProcessor;
    private final CamelContext camelContext;
    private final Optional<StickySessionCacheManager> stickySessionCacheManager;
    private final String capiContext;
    private final String reverseProxyHost;
    private final OkHttpClient okHttpClient;
    private final Optional<OpaService> opaService;

    public ServiceUtils(HttpUtils httpUtils,
                        Optional<Map<String, WebsocketClient>> websocketClientMap,
                        RouteUtils routeUtils,
                        MetricsProcessor metricsProcessor,
                        CamelContext camelContext,
                        Optional<StickySessionCacheManager> stickySessionCacheManager,
                        @Value("${camel.servlet.mapping.context-path}") String capiContext,
                        @Value("${capi.reverse.proxy.host}") String reverseProxyHost,
                        OkHttpClient okHttpClient,
                        Optional<OpaService> opaService) {
        this.httpUtils = httpUtils;
        this.websocketClientMap = websocketClientMap;
        this.routeUtils = routeUtils;
        this.metricsProcessor = metricsProcessor;
        this.camelContext = camelContext;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.capiContext = capiContext;
        this.reverseProxyHost = reverseProxyHost;
        this.okHttpClient = okHttpClient;
        this.opaService = opaService;

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
        try {
            List<String> apiRouteIdList = routeUtils.getAllRouteIdForAGivenService(existingService);
            for(String routeId : apiRouteIdList) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
                camelContext.getRouteController().stopRoute(Constants.CAMEL_REST_PREFIX + routeId);
                camelContext.removeRoute(Constants.CAMEL_REST_PREFIX + routeId);

                /*if(checkIfOpenApiIsEnabled(incomingService)) {

                    incomingService.setRoundRobinEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());
                    incomingService.setFailOverEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());
                    existingService.setRoundRobinEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());
                    existingService.setFailOverEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());

                    DirectRouteProcessor directRouteProcessor = new DirectRouteProcessor(camelContext, incomingService, routeUtils, metricsProcessor, routeId, capiContext, reverseProxyHost);
                    directRouteProcessor.setHttpUtils(httpUtils);
                    opaService.ifPresent(directRouteProcessor::setOpaService);
                    stickySessionCacheManager.ifPresent(directRouteProcessor::setStickySessionCacheManager);
                    directRouteProcessor.setServiceCache(serviceCache);
                    camelContext.addRoutes(directRouteProcessor);
                    camelContext.addRoutes(new RestDefinitionProcessor(camelContext, incomingService, routeUtils, routeId));

                    existingService.setMappingList(incomingService.getMappingList());
                    serviceCache.put(existingService.getId(), existingService);
                } else {
                    serviceCache.remove(existingService.getId());
                }*/
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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

    public void removeUnusedService(CamelContext camelContext, RouteUtils routeUtils, Cache<String, Service> serviceCache, List<String> serviceNameList) throws Exception {
        for (CacheEntry<String, Service> stringServiceCacheEntry : serviceCache.entries()) {
            Service service = stringServiceCacheEntry.getValue();
            if (!serviceNameList.contains(service.getName())) {
                serviceCache.remove(service.getId());
                if(service.getServiceMeta().getType().equals("websocket") && websocketClientMap.isPresent()) {
                    websocketClientMap.get().remove(service.getContext());
                } else {
                    List<String> serviceRouteIdList = routeUtils.getAllRouteIdForAGivenService(service);
                    for (String routeId : serviceRouteIdList) {
                        camelContext.getRouteController().stopRoute(routeId);
                        camelContext.removeRoute(routeId);
                        camelContext.getRouteController().stopRoute(Constants.CAMEL_REST_PREFIX + routeId);
                        camelContext.removeRoute(Constants.CAMEL_REST_PREFIX + routeId);
                    }
                }
            }
        }
    }

    public boolean checkIfOpenApiIsEnabled(Service service) {
        if(service.getServiceMeta() != null && service.getServiceMeta().getOpenApiEndpoint() != null && !service.getServiceMeta().getOpenApiEndpoint().isEmpty()) {
            try {
                Request request = new Request.Builder()
                        .url(service.getServiceMeta().getOpenApiEndpoint())
                        .build();
                Response response = okHttpClient.newCall(request).execute();

                log.trace("Calling Remote Open API Spec: {}", service.getServiceMeta().getOpenApiEndpoint());
                OpenAPI openAPI = new OpenAPIV3Parser().readContents(response.body().string()).getOpenAPI();
                service.setOpenAPI(openAPI);
                return true;
            } catch(Exception e) {
                log.warn("Open API specification is invalid for service {}", service.getId());
                return false;
            }
        } else {
            return true;
        }
    }
}