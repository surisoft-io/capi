package io.surisoft.capi.builder;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.processor.OpenApiProcessor;
import io.surisoft.capi.processor.StickyLoadBalancer;
import io.surisoft.capi.processor.TenantAwareLoadBalancer;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import io.surisoft.capi.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.rest.RestDefinition;
import org.cache2k.Cache;

public class DirectRouteProcessor extends RouteBuilder {
    private final RouteUtils routeUtils;
    private final Service service;
    private StickySessionCacheManager stickySessionCacheManager;
    private final String routeId;
    private final String capiContext;
    private final MetricsProcessor metricsProcessor;
    private final String reverseProxyHost;
    private OpaService opaService;
    private HttpUtils httpUtils;
    private Cache<String, Service> serviceCache;

    public DirectRouteProcessor(CamelContext camelContext, Service service, RouteUtils routeUtils, MetricsProcessor metricsProcessor, String routeId, String capiContext, String reverseProxyHost) {
        super(camelContext);
        this.service = service;
        this.routeUtils = routeUtils;
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
        routeUtils.buildOnExceptionDefinition(routeDefinition, service.getServiceMeta().isB3TraceId(), routeId);
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
                    .process(metricsProcessor)
                    .to(routeUtils.buildEndpoints(service))
                    .end()
                    .removeHeader(Constants.X_FORWARDED_HOST)
                    .removeHeader(Constants.X_FORWARDED_PREFIX)
                    .removeHeader(Constants.AUTHORIZATION_HEADER)
                    .removeHeader(Constants.CAPI_GROUP_HEADER)
                    .routeId(routeId);
        }
        routeUtils.registerMetric(routeId);
        routeUtils.registerTracer(service);

        //build the rest definition for the inline route, default since 4.5.0
        String restRouteId = Constants.CAMEL_REST_PREFIX + routeId;
        RestDefinition restDefinition = getRestDefinition(service);
        if(restDefinition != null) {
            restDefinition.to(Constants.CAMEL_DIRECT + routeId);
            restDefinition.routeId(restRouteId);
            routeUtils.registerMetric(restRouteId);
        } else {
            log.warn("Bad definition for service name: {}, please make sure the service context does not contain colons", service.getContext());
        }


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

    public void setStickySessionCacheManager(StickySessionCacheManager stickySessionCacheManager) {
        this.stickySessionCacheManager = stickySessionCacheManager;
    }

    private RestDefinition getRestDefinition(Service service) {
        RestDefinition restDefinition;
        service.setMatchOnUriPrefix(true);

        switch (routeUtils.getMethodFromRouteId(routeId)) {
            case "get" -> restDefinition = rest().get(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            case "post" -> restDefinition = rest().post(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix()
                    + Constants.MAP_HTTP_MESSAGE_FORM_URL_ENCODED_BODY);
            case "put" -> restDefinition = rest().put(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix()
                    + Constants.MAP_HTTP_MESSAGE_FORM_URL_ENCODED_BODY);
            case "delete" -> restDefinition = rest().delete(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            case "patch" -> restDefinition = rest().patch(routeUtils.buildFrom(service)
                    + Constants.MATCH_ON_URI_PREFIX
                    + service.isMatchOnUriPrefix());
            default -> {
                return null;
            }
        }
        return restDefinition;
    }
}