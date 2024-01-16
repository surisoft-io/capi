package io.surisoft.capi.utils;

import io.undertow.util.HttpString;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.Map;

public class Constants {
    private Constants() {
        throw new IllegalStateException("Utility class");
    }
    public static final String APPLICATION_NAME = "CAPI";
    public static final String FAIL_HTTP_REST_ENDPOINT_OBJECT = "http:%s?throwExceptionOnFailure=false&bridgeEndpoint=true&copyHeaders=true&connectionClose=true";
    public static final String FAIL_HTTPS_REST_ENDPOINT_OBJECT = "https:%s?throwExceptionOnFailure=false&bridgeEndpoint=true&copyHeaders=true&connectionClose=true";
    public static final String REASON_CODE_HEADER = "error-reason-code";
    public static final String REASON_MESSAGE_HEADER = "error-reason-message";
    public static final String ROUTE_ID_HEADER = "routeID";
    public static final String ORIGIN_HEADER = "Origin";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_REQUEST_PARAMETER = "access_token";
    public static final String COOKIE_HEADER = "Cookie";
    public static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final String AUTHORIZED_PARTY = "azp";
    public static final String CAPI_INTERNAL_ERROR = "capi-internal-error";
    public static final String CAPI_INTERNAL_ERROR_CLASS_NAME = "capi-internal-error-class-name";
    public static final String HTTP_CONNECT_TIMEOUT = "connectTimeout=";
    public static final String HTTP_SOCKET_TIMEOUT = "socketTimeout=";
    public static final String CUSTOM_HOST_HEADER = "customHostHeader=";
    public static final String ERROR_API_SHOW_TRACE_ID = "show-trace-id";
    public static final String ERROR_API_SHOW_INTERNAL_ERROR_MESSAGE = "show-internal-error-message";
    public static final String ERROR_API_SHOW_INTERNAL_ERROR_CLASS = "show-internal-error-class";
    public static final String TRACE_ID_HEADER = "X-B3-TraceId";
    public static final String CAPI_INTERNAL_REST_ERROR_PATH = "/capi-error";
    public static final String CAPI_URL_IN_ERROR = "HTTP_URL";
    public static final String CAPI_URI_IN_ERROR = "HTTP_URI";
    public static final String MATCH_ON_URI_PREFIX = "?matchOnUriPrefix=";
    public static final String X_FORWARDED_PREFIX = "X-Forwarded-Prefix";
    public static final String X_FORWARDED_HOST = "X-Forwarded-Host";
    public static final String CAMEL_DIRECT = "direct:";
    public static final String CAMEL_REST_PREFIX = "rd_";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_CODE = "errorCode";
    public static final String NO_CUSTOM_TRUST_STORE_PROVIDED = "No custom trust store was provided, to enable this feature, add a custom trust store.";
    public static final String STICKY_SESSION_IMAP_NAME = "stickySession";
    public static final String TENANT_HEADER = "tenant";
    public static final HttpString PROTOCOL_HTTP = new HttpString("HTTP/1.1");
    public static final String[] CAPI_WHITELISTED_PATHS = {
            //Swagger UI v2
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/META-INF/webjars/**",
            //Swagger UI v3 (OpenAPI)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            // Capi Specific
            "/capi/**",
            "/analytics/**",
            "/swagger/**"
    };
    public static final String[] CAPI_INTERNAL_ROUTES_PREFIX = {
            "rd_",
            "consul-discovery"
    };
    public static final String CAMEL_HTTP_SERVLET_REQUEST = "CamelHttpServletRequest";
    public static final String CACHE_ROUTE_STOPPED_EVENT = "RouteStoppedEvent";
    public static final String CACHE_ROUTE_REMOVED_EVENT = "RouteRemovedEvent";
    public static final String CACHE_EXCHANGE_FAILED_EVENT = "ExchangeFailedEvent";
    public static final String CAMEL_CLIENT_ENDPOINT_URL = "capi.client.endpoint.url";
    public static final String CAMEL_SERVER_ENDPOINT_URL = "capi.server.endpoint.url";
    public static final String CAMEL_SERVER_EXCHANGE_ID = "capi.server.exchange.id";
    public static final String CAMEL_CLIENT_EXCHANGE_ID = "capi.client.exchange.id";
    public static final String CAMEL_SERVER_EXCHANGE_FAILURE = "camel.server.exchange.failure";
    public static final String CAPI_EXCHANGE_REQUESTER_ID = "capi.requester.id";
    public static final String CAPI_REQUEST_METHOD = "capi.request.method";
    public static final String CAPI_REQUEST_CONTENT_TYPE = "capi.request.content.type";
    public static final String CAPI_REQUEST_CONTENT_LENGTH = "capi.request.content.length";
    public static final String CAPI_SERVER_EXCHANGE_MESSAGE_RESPONSE_CODE = "capi.server.exchange.message.response.code";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final int HTTPS_PORT = 443;
    public static final int HTTP_PORT = 80;
    public static final String CAPI_CONTEXT = "/capi";
    public static final String BEARER = "Bearer ";
    public static final String CAPI_GROUP_HEADER = "Capi-Group";
    public static final String WEBSOCKET_TYPE = "websocket";
    public static final String[] CAPI_ACCESS_CONTROL_ALLOW_HEADERS = {
            "Origin",
            "Accept",
            "X-Requested-With",
            "Content-Type",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "x-referrer",
            "Authorization",
            "Authorization-Propagation",
            "X-Csrf-Request",
            "Cache-Control",
            "pragma",
            "gem-context",
            "x-syncmode",
            "X-Total-Count",
            "Last-Event-ID",
            "X-B3-Sampled",
            "X-B3-SpanId",
            "X-B3-TraceId",
            "X-B3-ParentSpanId",
            "X-Auth-Url-Index",
            "X-Apigateway-Impersonated-Cookie-Name"
    };

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_VALUE = "GET, POST, DELETE, PUT, PATCH";
    public static final String OPTIONS_METHODS_VALUE = "OPTIONS";
    public static final String ACCESS_CONTROL_MAX_AGE_VALUE = "1728000";
    public static final Map<String, String> CAPI_CORS_MANAGED_HEADERS = Map.of(
            "Access-Control-Allow-Credentials", "true",
            "Access-Control-Allow-Methods", ACCESS_CONTROL_ALLOW_METHODS_VALUE,
            "Access-Control-Max-Age", ACCESS_CONTROL_MAX_AGE_VALUE,
            "Access-Control-Allow-Headers", StringUtils.join(CAPI_ACCESS_CONTROL_ALLOW_HEADERS, ",")
    );
}