package io.surisoft.capi.utils;

import io.surisoft.capi.builder.DirectRouteProcessor;
import io.surisoft.capi.builder.RestDefinitionProcessor;
import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.service.OpaService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ServiceUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);

    @Autowired
    private HttpUtils httpUtils;

    @Autowired(required = false)
    private Map<String, WebsocketClient> websocketClientMap;

    @Autowired
    private RouteUtils routeUtils;

    @Autowired
    private MetricsProcessor metricsProcessor;

    @Autowired
    private CamelContext camelContext;

    @Autowired(required = false)
    private StickySessionCacheManager stickySessionCacheManager;

    @Value("${camel.servlet.mapping.context-path}")
    private String capiContext;

    @Value("${capi.reverse.proxy.host}")
    private String reverseProxyHost;

    @Autowired(required = false)
    private OpaService opaService;

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

    public void updateExistingService(Service existingService,
                                      Service incomingService,
                                      Cache<String, Service> serviceCache) {

        if(isMappingChanged(existingService.getMappingList().stream().toList(), incomingService.getMappingList().stream().toList())) {
            log.trace("Changes detected for Service: {}, redeploying routes.", existingService.getId());
            try {
                List<String> apiRouteIdList = routeUtils.getAllRouteIdForAGivenService(existingService);
                for(String routeId : apiRouteIdList) {
                    camelContext.getRouteController().stopRoute(routeId);
                    camelContext.removeRoute(routeId);
                    camelContext.getRouteController().stopRoute(Constants.CAMEL_REST_PREFIX + routeId);
                    camelContext.removeRoute(Constants.CAMEL_REST_PREFIX + routeId);

                    incomingService.setRoundRobinEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());
                    incomingService.setFailOverEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());
                    existingService.setRoundRobinEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());
                    existingService.setFailOverEnabled(incomingService.getMappingList().size() != 1 && !incomingService.getServiceMeta().isTenantAware());

                    DirectRouteProcessor directRouteProcessor = new DirectRouteProcessor(camelContext, incomingService, routeUtils, metricsProcessor, routeId, stickySessionCacheManager, capiContext, reverseProxyHost);
                    directRouteProcessor.setHttpUtils(httpUtils);
                    directRouteProcessor.setOpaService(opaService);
                    directRouteProcessor.setServiceCache(serviceCache);
                    camelContext.addRoutes(directRouteProcessor);
                    camelContext.addRoutes(new RestDefinitionProcessor(camelContext, incomingService, routeUtils, routeId));

                    existingService.setMappingList(incomingService.getMappingList());
                    serviceCache.put(existingService.getId(), existingService);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }


        } else {
            log.trace("No changes detected for Service: {}.", existingService.getId());
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
                if(service.getServiceMeta().getType().equals("websocket")) {
                    // websocketClientMap.remove()

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

    public void checkIfOpenApiIsEnabled(Service service) {
        if(service.getServiceMeta() != null && service.getServiceMeta().getOpenApiEndpoint() != null && !service.getServiceMeta().getOpenApiEndpoint().isEmpty()) {
            try {
                OpenAPI openAPI = new OpenAPIV3Parser().read(service.getServiceMeta().getOpenApiEndpoint());
                service.setOpenAPI(openAPI);
            } catch(Exception e) {
                log.warn("Open API specification is invalid for service {}", service.getId());
            }
        }
    }
}