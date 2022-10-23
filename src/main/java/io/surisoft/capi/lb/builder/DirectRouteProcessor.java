package io.surisoft.capi.lb.builder;

import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.AuthorizationProcessor;
import io.surisoft.capi.lb.processor.MetricsProcessor;
import io.surisoft.capi.lb.processor.SessionChecker;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;

public class DirectRouteProcessor extends RouteBuilder {

    private final RouteUtils routeUtils;
    private final Api api;
    private final StickySessionCacheManager stickySessionCacheManager;
    private final String routeId;
    private final String capiContext;
    private final MetricsProcessor metricsProcessor;
    private final String reverseProxyHost;

    public DirectRouteProcessor(CamelContext camelContext, Api api, RouteUtils routeUtils, MetricsProcessor metricsProcessor, String routeId, StickySessionCacheManager stickySessionCacheManager, String capiContext, String reverseProxyHost) {
        super(camelContext);
        this.api = api;
        this.routeUtils = routeUtils;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.routeId = routeId;
        this.capiContext = capiContext;
        this.metricsProcessor = metricsProcessor;
        this.reverseProxyHost = reverseProxyHost;
    }

    @Override
    public void configure() {

        RouteDefinition routeDefinition = from("direct:" + routeId);

        if(api.isForwardPrefix()) {
            routeDefinition
                    .setHeader(Constants.X_FORWARDED_PREFIX, constant(capiContext + api.getContext()));
        }

        if(reverseProxyHost != null) {
            routeDefinition
                    .setHeader(Constants.X_FORWARDED_HOST, constant(reverseProxyHost));
        }

        log.trace("Trying to build and deploy route {}", routeId);
        routeUtils.buildOnExceptionDefinition(routeDefinition, api.isZipkinShowTraceId(), false, false, routeId);
        if(api.isSecured()) {
            routeUtils.enableAuthorization(api.getId(), routeDefinition);
        }
        if(api.isFailoverEnabled()) {
            routeDefinition
                    .process(metricsProcessor)
                    .loadBalance()
                    .failover(1, false, api.isRoundRobinEnabled(), false)
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        } else if(api.isStickySession()) {
            routeDefinition
                    .process(metricsProcessor)
                    .loadBalance(new SessionChecker(stickySessionCacheManager, api.getStickySessionParam(), api.isStickySessionParamInCookie()))
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        } else {
            routeDefinition
                    .process(metricsProcessor)
                    .loadBalance()
                    .roundRobin()
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        }
        routeUtils.registerMetric(routeId);
        api.setRouteId(routeId);
        routeUtils.registerTracer(api);
    }
}