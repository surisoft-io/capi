package io.surisoft.capi.utils;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.surisoft.capi.processor.AuthorizationProcessor;
import io.surisoft.capi.processor.HttpErrorProcessor;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.service.CapiTrustManager;
import io.surisoft.capi.tracer.CapiTracer;
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
import java.util.Map;

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
    private CapiTracer capiTracer;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private Cache<String, Service> serviceCache;
    @Autowired(required = false)
    private AuthorizationProcessor authorizationProcessor;
    @Autowired
    private WebsocketUtils websocketUtils;
    @Autowired(required = false)
    private Map<String, WebsocketClient> websocketClientMap;

    public void registerMetric(String routeId) {
        meterRegistry.counter(routeId);
    }

    public void registerTracer(Service service) {
        if (capiTracer != null) {
            log.debug("Adding API to tracer as {}", service.getId());
            capiTracer.addServerServiceMapping(service.getId(), service.getName());
        }
    }

    public void buildOnExceptionDefinition(RouteDefinition routeDefinition,
                                           boolean isTraceIdVisible,
                                           boolean isInternalExceptionMessageVisible,
                                           boolean isInternalExceptionVisible,
                                           String routeID) {
        routeDefinition
                .onException(Exception.class)
                .handled(true)
                .setHeader(Constants.ERROR_API_SHOW_TRACE_ID, constant(isTraceIdVisible))
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

    public String[] buildEndpoints(Service service) {
        List<String> transformedEndpointList = new ArrayList<>();
        for(Mapping mapping : service.getMappingList()) {
            HttpProtocol httpProtocol = null;
            if(service.getServiceMeta().getSchema() == null || service.getServiceMeta().getSchema().equals("http")) {
                httpProtocol = HttpProtocol.HTTP;
            } else {
                httpProtocol = HttpProtocol.HTTPS;
            }

            String endpoint;
            if(mapping.getPort() > -1) {
                endpoint = httpProtocol.getProtocol() + "://" + mapping.getHostname() + ":" + mapping.getPort() + mapping.getRootContext() + "?bridgeEndpoint=true&throwExceptionOnFailure=false";
            } else {
                endpoint = httpProtocol.getProtocol() + "://" + mapping.getHostname() + mapping.getRootContext() + "?bridgeEndpoint=true&throwExceptionOnFailure=false";
            }
            if(mapping.isIngress()) {
                endpoint = httpUtils.setIngressEndpoint(endpoint, mapping.getHostname());
            }

            if(service.getServiceMeta().isTenantAware()) {
                endpoint = endpoint + "&" + Constants.TENANT_HEADER + "="  + mapping.getTenandId();
            }
            transformedEndpointList.add(endpoint);
        }
        return transformedEndpointList.toArray(String[]::new);
    }

    public String buildFrom(Service service) {
        if(!service.getContext().startsWith("/")) {
            return "/" + service.getContext();
        }
        return service.getContext();
    }
    public List<String> getAllRouteIdForAGivenService(Service service) {
        List<String> routeIdList = new ArrayList<>();
        routeIdList.add(service.getId() + ":" + HttpMethod.DELETE.getMethod());
        routeIdList.add(service.getId() + ":" + HttpMethod.PUT.getMethod());
        routeIdList.add(service.getId() + ":" + HttpMethod.POST.getMethod());
        routeIdList.add(service.getId() + ":" + HttpMethod.GET.getMethod());
        routeIdList.add(service.getId() + ":" + HttpMethod.PATCH.getMethod());
        return routeIdList;
    }

    public List<String> getAllRouteIdForAGivenService(String serviceId) {
        List<String> routeIdList = new ArrayList<>();
        routeIdList.add(serviceId + ":" + HttpMethod.DELETE.getMethod());
        routeIdList.add(serviceId + ":" + HttpMethod.PUT.getMethod());
        routeIdList.add(serviceId + ":" + HttpMethod.POST.getMethod());
        routeIdList.add(serviceId + ":" + HttpMethod.GET.getMethod());
        routeIdList.add(serviceId + ":" + HttpMethod.PATCH.getMethod());
        return routeIdList;
    }

    public String getMethodFromRouteId(String routeId) {
        return routeId.split(":")[2];
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

    public boolean isStickySessionEnabled(Service service) {
        return service.getServiceMeta().isStickySession() && service.getServiceMeta().getStickySessionKey() != null && service.getServiceMeta().getStickySessionType() != null;
    }

    public boolean isStickySessionOnCookie(Service service) {
        return service.getServiceMeta().getStickySessionType().equals("cookie");
    }

    public void reloadTrustStoreManager(String serviceId, boolean undeploy) {
        try {
            log.trace("Reloading Trust Store Manager after changes for API: {}", serviceId);
            HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("https");
            CapiTrustManager capiTrustManager = (CapiTrustManager) httpComponent.getSslContextParameters().getTrustManagers().getTrustManager();
            capiTrustManager.reloadTrustManager();
            if(undeploy) {
                List<String> routeIdList = getAllRouteIdForAGivenService(serviceId);
                for(String routeId : routeIdList) {
                    camelContext.getRouteController().stopRoute(Constants.CAMEL_REST_PREFIX + routeId);
                    camelContext.removeRoute(Constants.CAMEL_REST_PREFIX + routeId);
                    camelContext.getRouteController().stopRoute(routeId);
                    camelContext.removeRoute(routeId);
                }
               serviceCache.remove(serviceId);
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