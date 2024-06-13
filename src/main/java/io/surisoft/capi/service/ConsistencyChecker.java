package io.surisoft.capi.service;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ConsistencyChecker {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyChecker.class);
    private final RouteUtils routeUtils;
    private final CamelContext camelContext;
    private final Cache<String, Service> serviceCache;

    public ConsistencyChecker(CamelContext camelContext, RouteUtils routeUtils, Cache<String, Service> serviceCache) {
        this.routeUtils = routeUtils;
        this.camelContext = camelContext;
        this.serviceCache = serviceCache;
    }

    public void process() {
        log.debug("Looking for inconsistent routes...");
        checkForOpenApiInconsistency();
    }

    private void checkForOpenApiInconsistency() {
        List<String> servicesToRemove = new ArrayList<>();
        serviceCache.entries().forEach(service -> {
            if(service.getValue().getServiceMeta().getOpenApiEndpoint() != null && service.getValue().getOpenAPI() == null) {
                log.warn("Inconsistency detected for service {}. Service routes will be destroyed.", service.getKey());
                List<String> serviceRouteIdList = routeUtils.getAllRouteIdForAGivenService(service.getValue());
                for (String routeId : serviceRouteIdList) {
                    try {
                        camelContext.getRouteController().stopRoute(routeId);
                        camelContext.removeRoute(routeId);
                        servicesToRemove.add(service.getKey());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }

            }
        });
        servicesToRemove.forEach(serviceCache::remove);
    }
}