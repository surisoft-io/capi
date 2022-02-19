package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.SessionChecker;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;

public class ConsulRouteProcessor extends RouteBuilder {

    private RouteUtils routeUtils;
    private Api api;
    private StickySessionCacheManager stickySessionCacheManager;
    private String routeId;

    public ConsulRouteProcessor(CamelContext camelContext, Api api, RouteUtils routeUtils, String routeId, StickySessionCacheManager stickySessionCacheManager) {
        super(camelContext);
        this.api = api;
        this.routeUtils = routeUtils;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.routeId = routeId;
    }

    @Override
    public void configure() {
        RouteDefinition routeDefinition = getRouteDefinition(api);
        log.trace("Trying to build and deploy route {}", routeId);
        routeUtils.buildOnExceptionDefinition(routeDefinition, false, false, false, routeId);
        if(api.isFailoverEnabled()) {
            routeDefinition
                    .loadBalance()
                    .failover(1, false, api.isRoundRobinEnabled(), false)
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        } else if(api.isStickySession()) {
            routeDefinition
                    .loadBalance(new SessionChecker(stickySessionCacheManager, api.getStickySessionParam(), api.isStickySessionParamInCookie()))
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        } else {
            routeDefinition
                    .loadBalance()
                    .roundRobin()
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        }
        routeUtils.registerMetric(routeId);
    }

    private RouteDefinition getRouteDefinition(Api api) {
        RouteDefinition routeDefinition = null;
        api.setMatchOnUriPrefix(true);
        switch (routeUtils.getMethodFromRouteId(routeId)) {
            case "get":
                routeDefinition = rest().get(routeUtils.buildFrom(api) + Constants.MATCH_ON_URI_PREFIX + api.isMatchOnUriPrefix()).route();
                break;
            case "post":
                routeDefinition = rest().post(routeUtils.buildFrom(api) + Constants.MATCH_ON_URI_PREFIX + api.isMatchOnUriPrefix()).route();
                break;
            case "put":
                routeDefinition = rest().put(routeUtils.buildFrom(api) + Constants.MATCH_ON_URI_PREFIX + api.isMatchOnUriPrefix()).route();
                break;
            case "delete":
                routeDefinition = rest().delete(routeUtils.buildFrom(api) + Constants.MATCH_ON_URI_PREFIX + api.isMatchOnUriPrefix()).route();
                break;
        }
        return routeDefinition;
    }
}