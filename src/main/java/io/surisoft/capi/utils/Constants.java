package io.surisoft.capi.utils;

import io.undertow.util.HttpString;

import java.util.List;
import java.util.Map;

public class Constants {
    private Constants() {
        throw new IllegalStateException("Utility class");
    }
    public static final String APPLICATION_NAME = "CAPI";
    public static final String REASON_CODE_HEADER = "error-reason-code";
    public static final String REASON_MESSAGE_HEADER = "error-reason-message";
    public static final String ROUTE_ID_HEADER = "routeID";
    public static final String ORIGIN_HEADER = "Origin";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_REQUEST_PARAMETER = "access_token";
    public static final String COOKIE_HEADER = "Cookie";
    public static final String AUTHORIZED_PARTY = "azp";
    public static final String CAPI_INTERNAL_ERROR = "capi-internal-error";
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
    public static final String CAPI_ERROR_ROUTE = "direct:error";
    public static final String CAMEL_REST_PREFIX = "rd_";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_CODE = "errorCode";
    public static final String NO_CUSTOM_TRUST_STORE_PROVIDED = "No custom trust store was provided, to enable this feature, add a custom trust store.";
    public static final String TENANT_HEADER = "tenant";
    public static final HttpString PROTOCOL_HTTP = new HttpString("HTTP/1.1");
    public static final String MAP_HTTP_MESSAGE_FORM_URL_ENCODED_BODY = "&mapHttpMessageFormUrlEncodedBody=false";
    public static final String BLUECOAT_HEADER = "X-BlueCoat-Via";
    public static final String UNDERSTOW_HEALTH_PATH = "/health";
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
    public static final List<String> CAPI_INTERNAL_ROUTES_PREFIX = List.of("consul-discovery-service",
                                                                           "consistency-checker-service");
    public static final String CAMEL_HTTP_SERVLET_REQUEST = "CamelHttpServletRequest";
    public static final String CAMEL_CLIENT_ENDPOINT_URL = "capi.client.endpoint.url";
    public static final String CAMEL_SERVER_ENDPOINT_URL = "capi.server.endpoint.url";
    public static final String CAMEL_SERVER_EXCHANGE_ID = "capi.server.exchange.id";
    public static final String CAPI_REQUESTER_TOKEN_ISSUER = "capi.requester.token.issuer";
    public static final String CAMEL_SERVER_EXCHANGE_FAILURE = "camel.server.exchange.failure";
    public static final String CAPI_EXCHANGE_REQUESTER_ID = "capi.token.requester.id";
    public static final String CAPI_TOKEN_EXPIRED = "capi.token.expired";
    public static final String CAPI_REQUEST_METHOD = "capi.request.method";
    public static final String CAPI_REQUEST_CONTENT_TYPE = "capi.request.content.type";
    public static final String CAPI_REQUEST_CONTENT_LENGTH = "capi.request.content.length";
    public static final String CAPI_REQUEST_ERROR_MESSAGE = "capi.request.error.message";
    public static final String CAPI_SERVER_EXCHANGE_MESSAGE_RESPONSE_CODE = "capi.server.exchange.message.response.code";
    public static final String CAPI_WS_CLIENT_HOST = "capi.ws.client.host";
    public static final String CAPI_WS_CLIENT_PORT = "capi.ws.client.port";
    public static final String CAPI_WS_CLIENT_PATH = "capi.ws.client.path";
    public static final String CAPI_WS_CLIENT_QUERY = "capi.ws.client.query";
    public static final String CAPI_WS_CLIENT_SCHEME = "capi.ws.client.scheme";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT_TYPE = "Accept";
    public static final HttpString HTTP_STRING_CONTENT_TYPE = new HttpString("Content-Type");
    public static final int HTTPS_PORT = 443;
    public static final int HTTP_PORT = 80;
    public static final String CAPI_CONTEXT = "/capi";
    public static final String BEARER = "Bearer ";
    public static final String CAPI_GROUP_HEADER = "Capi-Group";
    public static final String WEBSOCKET_TYPE = "websocket";
    public static final String SSE_TYPE = "sse";
    public static final String FULL_TYPE = "full";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_VALUE = "GET, POST, DELETE, PUT, PATCH";
    public static final String OPTIONS_METHODS_VALUE = "OPTIONS";
    public static final String ACCESS_CONTROL_MAX_AGE_VALUE = "86400";
    public static final Map<String, String> CAPI_CORS_MANAGED_HEADERS = Map.of(
            "Access-Control-Allow-Credentials", "true",
            "Access-Control-Allow-Methods", ACCESS_CONTROL_ALLOW_METHODS_VALUE,
            "Access-Control-Max-Age", ACCESS_CONTROL_MAX_AGE_VALUE
    );
    public static final String UNDERTOW_LISTENING_ADDRESS = "0.0.0.0";
    public static final String ERROR_LISTENING_ADDRESS = "0.0.0.0";
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
    public static final int BAD_REQUEST_CODE = 400;
    public static final int NOT_FOUND_CODE = 404;
    public static final String CLIENT_START_TIME = "ClientStartTime";
    public static final String CLIENT_END_TIME = "ClientEndTime";
    public static final String CLIENT_ENDPOINT = "ClientEndpoint";
    public static final String CLIENT_RESPONSE_CODE = "ClientResponseCode";
    public static final String CONSUL_KV_STORE_API = "/v1/kv/";
    public static final String CAPI_CORS_HEADERS_CACHE_KEY = "capi-cors-headers";
    public static final String CONSUL_CAPI_TRUST_STORE_GROUP_KEY = "capi-trust-store";
    public static final String CAPI_META_THROTTLE_CONSUMER_KEY = "Capi-Meta-Throttle-Consumer-Key";
    public static final String CAPI_SHOULD_THROTTLE = "Capi-Should-Throttle";
    public static final String CAPI_THROTTLE_DURATION_MILLI = "Capi-Throttle-Duration";
    public static final String CAPI_META_THROTTLE_DURATION = "Capi-Meta-Throttle-Duration";
    public static final String CAPI_META_THROTTLE_TOTAL_CALLS_ALLOWED = "Capi-Meta-Throttle-Total-Calls-Allowed";
    public static final String CAPI_META_THROTTLE_CURRENT_CALL_NUMBER = "Capi-Meta-Throttle-Current-Call-Number";
}