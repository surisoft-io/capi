package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.MetricsProcessor;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestDefinition;

public class ConsulRouteProcessor extends RouteBuilder {

    private RouteUtils routeUtils;
    private Api api;
    private StickySessionCacheManager stickySessionCacheManager;
    private String routeId;
    private String capiContext;
    private MetricsProcessor metricsProcessor;

    public ConsulRouteProcessor(CamelContext camelContext, Api api, RouteUtils routeUtils, MetricsProcessor metricsProcessor, String routeId, StickySessionCacheManager stickySessionCacheManager, String capiContext) {
        super(camelContext);
        this.api = api;
        this.routeUtils = routeUtils;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.routeId = routeId;
        this.capiContext = capiContext;
        this.metricsProcessor = metricsProcessor;
    }

    @Override
    public void configure() {

        //RouteDefinition routeDefinition = new RouteDefinition();

        /*if(api.isForwardPrefix()) {
            routeDefinition.setHeader(Constants.X_FORWARDED_PREFIX, constant(capiContext + api.getContext()));
        }
        log.trace("Trying to build and deploy route {}", routeId);
        routeUtils.buildOnExceptionDefinition(routeDefinition, api.isZipkinShowTraceId(), false, false, routeId);
        if(api.isFailoverEnabled()) {
            routeDefinition
                    .from("direct:" + routeId)
                    .process(metricsProcessor)
                    .loadBalance()
                    .failover(1, false, api.isRoundRobinEnabled(), false)
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        } else if(api.isStickySession()) {
            routeDefinition
                    .from("direct:" + routeId)
                    .process(metricsProcessor)
                    .loadBalance(new SessionChecker(stickySessionCacheManager, api.getStickySessionParam(), api.isStickySessionParamInCookie()))
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        } else {
            routeDefinition
                    .from("direct:" + routeId)
                    .process(metricsProcessor)
                    .loadBalance()
                    .roundRobin()
                    .to(routeUtils.buildEndpoints(api))
                    .end()
                    .routeId(routeId);
        }
        routeUtils.registerMetric(routeId);
        api.setRouteId(routeId);
        routeUtils.registerTracer(api);*/


        RestDefinition restDefinition = getRestDefinition(api);
        restDefinition.to("direct:" + routeId);
        restDefinition.id("x_" + routeId);

        routeUtils.registerMetric("x_" + routeId);
    }

    private RestDefinition getRestDefinition(Api api) {
        RestDefinition restDefinition = null;
        api.setMatchOnUriPrefix(true);

        switch (routeUtils.getMethodFromRouteId(routeId)) {
            case "get":
                restDefinition = rest().get(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix()); //.route();
                break;
            case "post":
                restDefinition = rest().post(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix()); //.route();
                break;
            case "put":
                restDefinition = rest().put(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix()); //.route();
                break;
            case "delete":
                restDefinition = rest().delete(routeUtils.buildFrom(api)
                        + Constants.MATCH_ON_URI_PREFIX
                        + api.isMatchOnUriPrefix()); //.route();
                break;
        }
        return restDefinition;
    }
}