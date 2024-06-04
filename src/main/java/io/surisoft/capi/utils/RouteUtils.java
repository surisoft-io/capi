package io.surisoft.capi.utils;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.AuthorizationProcessor;
import io.surisoft.capi.processor.HttpErrorProcessor;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.service.CapiTrustManager;
import io.surisoft.capi.tracer.CapiTracer;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.apache.camel.language.constant.ConstantLanguage.constant;

@Component
public class RouteUtils {
    private static final Logger log = LoggerFactory.getLogger(RouteUtils.class);
    private boolean sslEnabled;
    private final String capiGatewayErrorEndpoint;
    private final boolean capiGatewayErrorEndpointSsl;
    private final HttpErrorProcessor httpErrorProcessor;
    private final HttpUtils httpUtils;
    private final CompositeMeterRegistry meterRegistry;
    private final Optional<CapiTracer> capiTracer;
    private final CamelContext camelContext;
    private Cache<String, Service> serviceCache;
    private final Optional<AuthorizationProcessor> authorizationProcessor;
    private WebsocketUtils websocketUtils;
    private Map<String, WebsocketClient> websocketClientMap;
    private final boolean gatewayCorsManagementEnabled;
    private final boolean capiErrorListenerEnabled;
    private final String capiErrorListenerContext;
    private final int capiErrorListenerPort;

    public RouteUtils(@Value("${server.ssl.enabled}") boolean sslEnabled,
                      @Value("${capi.gateway.error.endpoint}") String capiGatewayErrorEndpoint,
                      @Value("${capi.gateway.error.ssl}") boolean capiGatewayErrorEndpointSsl,
                      HttpErrorProcessor httpErrorProcessor,
                      HttpUtils httpUtils,
                      CompositeMeterRegistry meterRegistry,
                      Optional<CapiTracer> capiTracer,
                      CamelContext camelContext,
                      Cache<String, Service> serviceCache,
                      Optional<AuthorizationProcessor> authorizationProcessor,
                      WebsocketUtils websocketUtils,
                      Map<String, WebsocketClient> websocketClientMap,
                      @Value("${capi.gateway.cors.management.enabled}") boolean gatewayCorsManagementEnabled,
                      @Value("${capi.gateway.error.listener.enabled}") boolean capiErrorListenerEnabled,
                      @Value("${capi.gateway.error.listener.context}") String capiErrorListenerContext,
                      @Value("${capi.gateway.error.listener.port}") int capiErrorListenerPort) {
        this.sslEnabled = sslEnabled;
        this.capiGatewayErrorEndpoint = capiGatewayErrorEndpoint;
        this.capiGatewayErrorEndpointSsl = capiGatewayErrorEndpointSsl;
        this.httpErrorProcessor = httpErrorProcessor;
        this.httpUtils = httpUtils;
        this.meterRegistry = meterRegistry;
        this.capiTracer = capiTracer;
        this.camelContext = camelContext;
        this.serviceCache = serviceCache;
        this.authorizationProcessor = authorizationProcessor;
        this.websocketUtils = websocketUtils;
        this.websocketClientMap = websocketClientMap;
        this.gatewayCorsManagementEnabled = gatewayCorsManagementEnabled;
        this.capiErrorListenerEnabled = capiErrorListenerEnabled;
        this.capiErrorListenerContext = capiErrorListenerContext;
        this.capiErrorListenerPort = capiErrorListenerPort;

    }

    public void registerMetric(String routeId) {
        meterRegistry.counter(routeId);
    }

    public void registerTracer(Service service) {
        if (capiTracer.isPresent()) {
            log.debug("Adding API to tracer as {}", service.getId());
            capiTracer.get().addServerServiceMapping(service.getId(), service.getName());
        }
    }

    public void buildOnExceptionDefinition(RouteDefinition routeDefinition,
                                           boolean isTraceIdVisible,
                                           String routeID) {

        if(capiErrorListenerEnabled) {
            routeDefinition
                    .onException(Exception.class)
                    .handled(true)
                    .setHeader(Constants.ERROR_API_SHOW_TRACE_ID, constant(isTraceIdVisible))
                    .process(httpErrorProcessor)
                    .setHeader(Constants.ROUTE_ID_HEADER, constant(routeID))
                    .toF(Constants.FAIL_HTTP_REST_ENDPOINT_OBJECT, "localhost:" + capiErrorListenerPort + capiErrorListenerContext)
                    .removeHeader(Constants.ERROR_API_SHOW_TRACE_ID)
                    .removeHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_MESSAGE)
                    .removeHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_CLASS)
                    .removeHeader(Constants.CAPI_URL_IN_ERROR)
                    .removeHeader(Constants.CAPI_URI_IN_ERROR)
                    .removeHeader(Constants.ROUTE_ID_HEADER)
                    .removeHeader(Constants.REASON_CODE_HEADER)
                    .removeHeader(Constants.REASON_MESSAGE_HEADER)
                    .end();
        } else {
            routeDefinition
                    .onException(Exception.class)
                    .handled(true)
                    .setHeader(Constants.ERROR_API_SHOW_TRACE_ID, constant(isTraceIdVisible))
                    .process(httpErrorProcessor)
                    .setHeader(Constants.ROUTE_ID_HEADER, constant(routeID))
                    .toF((capiGatewayErrorEndpointSsl ? Constants.FAIL_HTTPS_REST_ENDPOINT_OBJECT : Constants.FAIL_HTTP_REST_ENDPOINT_OBJECT), capiGatewayErrorEndpoint)
                    .removeHeader(Constants.ERROR_API_SHOW_TRACE_ID)
                    .removeHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_MESSAGE)
                    .removeHeader(Constants.ERROR_API_SHOW_INTERNAL_ERROR_CLASS)
                    .removeHeader(Constants.CAPI_URL_IN_ERROR)
                    .removeHeader(Constants.CAPI_URI_IN_ERROR)
                    .removeHeader(Constants.ROUTE_ID_HEADER)
                    .removeHeader(Constants.REASON_CODE_HEADER)
                    .removeHeader(Constants.REASON_MESSAGE_HEADER)
                    .end();
        }
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

            if(gatewayCorsManagementEnabled) {
                endpoint = endpoint + "&headerFilterStrategy=#capiCorsFilterStrategy";
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

    public boolean isStickySessionEnabled(Service service, StickySessionCacheManager stickySessionCacheManager) {
        return service.getServiceMeta().isStickySession() &&
                service.getServiceMeta().getStickySessionKey() != null &&
                service.getServiceMeta().getStickySessionType() != null &&
                stickySessionCacheManager != null;
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
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void enableAuthorization(String apiId, RouteDefinition routeDefinition) {
        if(authorizationProcessor.isPresent()) {
            routeDefinition.process(this.authorizationProcessor.get());
        } else {
            log.warn("The api with id {} is marked to protect but there is no OIDC provider enabled.", apiId);
        }
    }
}