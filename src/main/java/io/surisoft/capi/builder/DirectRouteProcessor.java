package io.surisoft.capi.builder;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.processor.StickyLoadBalancer;
import io.surisoft.capi.processor.OpenApiProcessor;
import io.surisoft.capi.processor.TenantAwareLoadBalancer;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import io.surisoft.capi.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.cache2k.Cache;

public class DirectRouteProcessor extends RouteBuilder {
    private final RouteUtils routeUtils;
    private final Service service;
    private final StickySessionCacheManager stickySessionCacheManager;
    private final String routeId;
    private final String capiContext;
    private final MetricsProcessor metricsProcessor;
    private final String reverseProxyHost;
    private OpaService opaService;
    private HttpUtils httpUtils;
    private Cache<String, Service> serviceCache;

    public DirectRouteProcessor(CamelContext camelContext, Service service, RouteUtils routeUtils, MetricsProcessor metricsProcessor, String routeId, StickySessionCacheManager stickySessionCacheManager, String capiContext, String reverseProxyHost) {
        super(camelContext);
        this.service = service;
        this.routeUtils = routeUtils;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.routeId = routeId;
        this.capiContext = capiContext;
        this.metricsProcessor = metricsProcessor;
        this.reverseProxyHost = reverseProxyHost;
    }

    @Override
    public void configure() {
        RouteDefinition routeDefinition = from(Constants.CAMEL_DIRECT + routeId);
        if(reverseProxyHost != null) {
            routeDefinition
                    .setHeader(Constants.X_FORWARDED_HOST, constant(reverseProxyHost));
            routeDefinition
                    .setHeader(Constants.X_FORWARDED_PREFIX, constant(capiContext + service.getContext()));
        }

        if(service.getServiceMeta().getOpenApiEndpoint() != null && service.getOpenAPI() != null) {
            routeDefinition.process(new OpenApiProcessor(service.getOpenAPI(), httpUtils, serviceCache, opaService));
        }

        log.trace("Trying to build and deploy route {}", routeId);
        routeUtils.buildOnExceptionDefinition(routeDefinition, service.getServiceMeta().isB3TraceId(), true, false, routeId);
        if(service.getServiceMeta().isSecured()) {
            routeUtils.enableAuthorization(service.getId(), routeDefinition);
        }

        if(service.getServiceMeta().isKeepGroup()) {
            routeDefinition.setHeader(Constants.CAPI_GROUP_HEADER, constant(service.getContext()));
        }

        if(service.isFailOverEnabled()) {
            routeDefinition
                    .process(metricsProcessor)
                    .loadBalance()
                    .failover(1, false, service.isRoundRobinEnabled(), false)
                    .to(routeUtils.buildEndpoints(service))
                    .end()
                    .removeHeader(Constants.X_FORWARDED_HOST)
                    .removeHeader(Constants.X_FORWARDED_PREFIX)
                    .removeHeader(Constants.AUTHORIZATION_HEADER)
                    .removeHeader(Constants.CAPI_GROUP_HEADER)
                    .routeId(routeId);
        } else if(routeUtils.isStickySessionEnabled(service, stickySessionCacheManager)) {
            routeDefinition
                    .process(metricsProcessor)
                    .loadBalance(new StickyLoadBalancer(stickySessionCacheManager, service.getServiceMeta().getStickySessionKey(), routeUtils.isStickySessionOnCookie(service)))
                    .to(routeUtils.buildEndpoints(service))
                    .end()
                    .removeHeader(Constants.X_FORWARDED_HOST)
                    .removeHeader(Constants.X_FORWARDED_PREFIX)
                    .removeHeader(Constants.AUTHORIZATION_HEADER)
                    .removeHeader(Constants.CAPI_GROUP_HEADER)
                    .routeId(routeId);
        } else if(service.getServiceMeta().isTenantAware()) {
            routeDefinition
                    .process(metricsProcessor)
                    .loadBalance(new TenantAwareLoadBalancer())
                    .to(routeUtils.buildEndpoints(service))
                    .end()
                    .removeHeader(Constants.X_FORWARDED_HOST)
                    .removeHeader(Constants.X_FORWARDED_PREFIX)
                    .removeHeader(Constants.AUTHORIZATION_HEADER)
                    .removeHeader(Constants.CAPI_GROUP_HEADER)
                    .routeId(routeId);
        } else {
            routeDefinition
                    .to("metrics:timer:simple.timer?action=start")
                    .process(metricsProcessor)
                    .to(routeUtils.buildEndpoints(service))
                    .to("metrics:timer:simple.timer?action=stop")
                    .end()
                    .removeHeader(Constants.X_FORWARDED_HOST)
                    .removeHeader(Constants.X_FORWARDED_PREFIX)
                    .removeHeader(Constants.AUTHORIZATION_HEADER)
                    .removeHeader(Constants.CAPI_GROUP_HEADER)
                    .routeId(routeId);
        }
        routeUtils.registerMetric(routeId);
        //api.setRouteId(routeId);
        routeUtils.registerTracer(service);
    }

    public void setOpaService(OpaService opaService) {
        this.opaService = opaService;
    }

    public void setHttpUtils(HttpUtils httpUtils) {
        this.httpUtils = httpUtils;
    }

    public void setServiceCache(Cache<String, Service> serviceCache) {
        this.serviceCache = serviceCache;
    }
}