package io.surisoft.capi.metrics;

import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.RouteDetailsEndpointInfo;
import io.surisoft.capi.schema.RouteEndpointInfo;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.RouteUtils;
import io.surisoft.capi.utils.ServiceUtils;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Endpoint(id = "routes")
public class Routes {

    private final ServiceUtils serviceUtils;
    private final Cache<String, Service> serviceCache;
    private final CamelContext camelContext;
    private final RouteUtils routeUtils;
    private final MetricsProcessor metricsProcessor;

    public Routes(ServiceUtils serviceUtils,
                  Cache<String, Service> serviceCache,
                  CamelContext camelContext,
                  RouteUtils routeUtils,
                  MetricsProcessor metricsProcessor) {
        this.serviceUtils = serviceUtils;
        this.serviceCache = serviceCache;
        this.camelContext = camelContext;
        this.routeUtils = routeUtils;
        this.metricsProcessor = metricsProcessor;
    }

    @ReadOperation
    public Service getCachedService(@Selector String serviceName) {
        if(serviceCache.containsKey(serviceName)) {
            return serviceCache.get(serviceName);
        }
        return null;
    }

    @ReadOperation
    public List<RouteDetailsEndpointInfo> getAllRoutesInfo() {
        List<RouteDetailsEndpointInfo> detailInfoList = new ArrayList<>();
        List<RouteEndpointInfo> routeEndpointInfoList = camelContext.getRoutes().stream()
                .map(RouteEndpointInfo::new)
                .toList();
        for(RouteEndpointInfo routeEndpointInfo : routeEndpointInfoList) {
            if(!Constants.CAPI_INTERNAL_ROUTES_PREFIX.contains(routeEndpointInfo.getId()))  {
                detailInfoList.add(new RouteDetailsEndpointInfo(camelContext, camelContext.getRoute(routeEndpointInfo.getId())));
            }
        }
        return detailInfoList;
    }
}