package io.surisoft.capi.lb.utils;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.surisoft.capi.lb.builder.DirectRouteProcessor;
import io.surisoft.capi.lb.builder.RestDefinitionProcessor;
import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.AuthorizationProcessor;
import io.surisoft.capi.lb.zipkin.CapiZipkinTracer;
import io.surisoft.capi.lb.processor.HttpErrorProcessor;
import io.surisoft.capi.lb.processor.MetricsProcessor;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.HttpMethod;
import io.surisoft.capi.lb.schema.HttpProtocol;
import io.surisoft.capi.lb.schema.Mapping;

import io.surisoft.capi.lb.service.CapiTrustManager;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.model.RouteDefinition;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.apache.camel.language.constant.ConstantLanguage.constant;

@Component
public class RouteUtils {

    private static final Logger log = LoggerFactory.getLogger(RouteUtils.class);
    @Value("${capi.gateway.error.endpoint}")
    private String capiGatewayErrorEndpoint;
    @Autowired
    private HttpErrorProcessor httpErrorProcessor;
    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private CompositeMeterRegistry meterRegistry;
    @Autowired(required = false)
    private CapiZipkinTracer capiZipkinTracer;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private Cache<String, Api> apiCache;
    @Autowired(required = false)
    private AuthorizationProcessor authorizationProcessor;

    public void registerMetric(String routeId) {
        meterRegistry.counter(routeId);
    }

    public void registerTracer(Api api) {
        if (capiZipkinTracer != null) {
            log.debug("Adding API to Zipkin tracer as {}", api.getRouteId());
            capiZipkinTracer.addServerServiceMapping(api.getRouteId(), api.getZipkinServiceName() != null ? api.getZipkinServiceName() : api.getRouteId());
        }
    }

    public void buildOnExceptionDefinition(RouteDefinition routeDefinition,
                                           boolean isZipkinTraceIdVisible,
                                           boolean isInternalExceptionMessageVisible,
                                           boolean isInternalExceptionVisible,
                                           String routeID) {
        routeDefinition
                .onException(Exception.class)
                .handled(true)
                .setHeader(Constants.ERROR_API_SHOW_TRACE_ID, constant(isZipkinTraceIdVisible))
                .setHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_MESSAGE, constant(isInternalExceptionMessageVisible))
                .setHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_CLASS, constant(isInternalExceptionVisible))
                .process(httpErrorProcessor)
                .setHeader(Constants.ROUTE_ID_HEADER, constant(routeID))
                .toF(Constants.FAIL_REST_ENDPOINT_OBJECT, capiGatewayErrorEndpoint)
                .removeHeader(Constants.ERROR_API_SHOW_TRACE_ID)
                .removeHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_MESSAGE)
                .removeHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_CLASS)
                .removeHeader(Constants.CAPI_URL_IN_ERROR)
                .removeHeader(Constants.CAPI_URI_IN_ERROR)
                .removeHeader(Constants.ROUTE_ID_HEADER)
                .end();
    }

    public String[] buildEndpoints(Api api) {
        List<String> transformedEndpointList = new ArrayList<>();
        for(Mapping mapping : api.getMappingList()) {
            if(api.getHttpProtocol() == null) {
                api.setHttpProtocol(HttpProtocol.HTTP);
            }
            String endpoint;
            if(mapping.getPort() > -1) {
                endpoint = api.getHttpProtocol().getProtocol() + "://" + mapping.getHostname() + ":" + mapping.getPort() + mapping.getRootContext() + "?bridgeEndpoint=true&throwExceptionOnFailure=false";
            } else {
                endpoint = api.getHttpProtocol().getProtocol() + "://" + mapping.getHostname() + mapping.getRootContext() + "?bridgeEndpoint=true&throwExceptionOnFailure=false";
            }
            if(mapping.isIngress()) {
                endpoint = httpUtils.setIngressEndpoint(endpoint, mapping.getHostname());
            }

            if(api.isTenantAware()) {
                endpoint = endpoint + "&tenantId=" + mapping.getTenandId();
            }
            transformedEndpointList.add(endpoint);
        }
        return transformedEndpointList.toArray(String[]::new);
    }

    public String buildFrom(Api api) {
        if(!api.getContext().startsWith("/")) {
            return "/" + api.getContext();
        }
        return api.getContext();
    }

    public String getRouteId(Api api, String httpMethod) {
        return api.getName() + ":" + api.getContext() + ":" + httpMethod;
    }

    public List<String> getAllRouteIdForAGivenApi(Api api) {
        List<String> routeIdList = new ArrayList<>();
        routeIdList.add(api.getId() + ":" + HttpMethod.DELETE.getMethod());
        routeIdList.add(api.getId() + ":" + HttpMethod.PUT.getMethod());
        routeIdList.add(api.getId() + ":" + HttpMethod.POST.getMethod());
        routeIdList.add(api.getId() + ":" + HttpMethod.GET.getMethod());
        routeIdList.add(api.getId() + ":" + HttpMethod.PATCH.getMethod());
        return routeIdList;
    }

    public List<String> getAllRouteIdForAGivenApi(String apiId) {
        List<String> routeIdList = new ArrayList<>();
        routeIdList.add(apiId + ":" + HttpMethod.DELETE.getMethod());
        routeIdList.add(apiId + ":" + HttpMethod.PUT.getMethod());
        routeIdList.add(apiId + ":" + HttpMethod.POST.getMethod());
        routeIdList.add(apiId + ":" + HttpMethod.GET.getMethod());
        routeIdList.add(apiId + ":" + HttpMethod.PATCH.getMethod());
        return routeIdList;
    }

    public String getMethodFromRouteId(String routeId) {
        return routeId.split(":")[2];
    }

    public Api setApiDefaults(Api api) {
        //api.setConnectTimeout(5000);
        //api.setSocketTimeout(5000);
        //temp to false
        api.setFailoverEnabled(false);
        api.setMatchOnUriPrefix(true);
        api.setRoundRobinEnabled(true);
        api.setMaximumFailoverAttempts(1);
        return api;
    }

    public String getStickySessionId(String paramName, String paramValue) {
        return new String(Base64.getEncoder().encode((paramName + paramValue).getBytes()));
    }

    public List<String> getAllActiveRoutes(CamelContext camelContext) {
        List<String> routeIdList = new ArrayList<>();
        List<Route> routeList = camelContext.getRoutes();
        for(Route route : routeList) {
            routeIdList.add(route.getRouteId());
        }
        return routeIdList;
    }

    public void createRoute(Api incomingApi, Cache<String, Api> apiCache, CamelContext camelContext, MetricsProcessor metricsProcessor, StickySessionCacheManager stickySessionCacheManager, String capiContext, String reverseProxyHost) {
        apiCache.put(incomingApi.getId(), incomingApi);
        List<String> apiRouteIdList = getAllRouteIdForAGivenApi(incomingApi);
        for(String routeId : apiRouteIdList) {
            Route existingRoute = camelContext.getRoute(routeId);
            if(existingRoute == null) {
                try {
                    camelContext.addRoutes(new RestDefinitionProcessor(camelContext, incomingApi, this, routeId));
                    camelContext.addRoutes(new DirectRouteProcessor(camelContext, incomingApi, this, metricsProcessor, routeId, stickySessionCacheManager, capiContext, reverseProxyHost));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void reloadTrustStoreManager(String apiId, boolean undeploy) {
        try {
            log.trace("Reloading Trust Store Manager after changes for API: {}", apiId);
            HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("https");
            CapiTrustManager capiTrustManager = (CapiTrustManager) httpComponent.getSslContextParameters().getTrustManagers().getTrustManager();
            capiTrustManager.reloadTrustManager();
            if(undeploy) {
                List<String> routeIdList = getAllRouteIdForAGivenApi(apiId);
                for(String routeId : routeIdList) {
                    camelContext.getRouteController().stopRoute(Constants.CAMEL_REST_PREFIX + routeId);
                    camelContext.removeRoute(Constants.CAMEL_REST_PREFIX + routeId);
                    camelContext.getRouteController().stopRoute(routeId);
                    camelContext.removeRoute(routeId);
                }
                apiCache.remove(apiId);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void enableAuthorization(String apiId, RouteDefinition routeDefinition) {
        if(this.authorizationProcessor != null) {
            routeDefinition.process(this.authorizationProcessor);
        } else {
            log.warn("The api with id {} is marked to protect but there is no OIDC provider enabled.", apiId);
        }
    }
}