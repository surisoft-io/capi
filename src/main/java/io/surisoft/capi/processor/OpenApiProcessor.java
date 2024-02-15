package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.vavr.collection.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;

public class OpenApiProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(OpenApiProcessor.class);
    private final OpenAPI openAPI;
    private final HttpUtils httpUtils;
    private final Cache<String, Service> serviceCache;
    private final OpaService opaService;

    public OpenApiProcessor(OpenAPI openAPI, HttpUtils httpUtils, Cache<String, Service> serviceCache, OpaService opaService) {
        this.openAPI = openAPI;
        this.httpUtils = httpUtils;
        this.serviceCache = serviceCache;
        this.opaService = opaService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        if(!validateRequest(exchange)) {
            sendException("Call not allowed", Constants.BAD_REQUEST_CODE, exchange);
        }
    }

    public boolean validateRequest(Exchange exchange) {
        String callingPath = (String) exchange.getIn().getHeader("CamelHttpPath");
        String callingMethod = (String) exchange.getIn().getHeader("CamelHttpMethod");

        for (String path : openAPI.getPaths().keySet()) {
            // Check if the requestPath matches any of the defined paths
            if (isPathMatch(callingPath, path)) {
                Operation operation = switch (callingMethod.toLowerCase()) {
                    case "get" -> openAPI.getPaths().get(path).getGet();
                    case "post" -> openAPI.getPaths().get(path).getPost();
                    case "put" -> openAPI.getPaths().get(path).getPut();
                    case "patch" -> openAPI.getPaths().get(path).getPatch();
                    case "delete" -> openAPI.getPaths().get(path).getDelete();
                    default -> null;
                };
                if (operation != null) {
                    // The provided HTTP method is allowed for this path
                    // You can also perform additional validation for the request here
                    // (e.g., validate path parameters, request body, and response)
                    if(operation.getSecurity() != null) {
                        String accessToken = httpUtils.processAuthorizationAccessToken(exchange);
                        if(accessToken != null) {
                            String contextPath = (String) exchange.getIn().getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH);
                            Service service = serviceCache.get(httpUtils.contextToRole(contextPath));
                            if(service != null) {
                                if(!httpUtils.isAuthorized(accessToken, contextPath, service, opaService)) {
                                    sendException("Invalid authentication", Constants.FORBIDDEN_CODE, exchange);
                                }
                            } else {
                                sendException("Call not allowed", Constants.BAD_REQUEST_CODE, exchange);
                            }
                        } else {
                            sendException("No authorization provided", Constants.UNAUTHORIZED_CODE, exchange);
                        }
                        propagateAuthorization(exchange, accessToken);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPathMatch(String requestPath, String definedPath) {
        // Remove leading slashes
        while (requestPath.startsWith("/")) {
            requestPath = requestPath.substring(1);
        }

        // Remove trailing slashes
        while (requestPath.endsWith("/")) {
            requestPath = requestPath.substring(0, requestPath.length() - 1);
        }

        while (definedPath.startsWith("/")) {
            definedPath = definedPath.substring(1);
        }

        // Remove trailing slashes
        while (definedPath.endsWith("/")) {
            definedPath = definedPath.substring(0, requestPath.length() - 1);
        }

        // Split paths by '/' to handle individual segments
        String[] requestPathSegments = requestPath.split("/");
        String[] definedPathSegments = definedPath.split("/");

        if (requestPathSegments.length != definedPathSegments.length) {
            return false; // Paths have different segment counts
        }

        for (int i = 0; i < requestPathSegments.length; i++) {
            if (!definedPathSegments[i].equals(requestPathSegments[i]) && !isPathParameter(definedPathSegments[i])) {
                return false; // Segment doesn't match and is not a path parameter
            }
        }
        return true; // Request path matches the defined path
    }

    // Helper method to check if a path segment is a path parameter (contains curly braces)
    private boolean isPathParameter(String pathSegment) {
        return pathSegment.matches("\\{.*\\}");
    }

    private void sendException(String message, int errorCode, Exchange exchange) {
        exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, errorCode);
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, message);
        exchange.setException(new AuthorizationException(message));
    }

    private void propagateAuthorization(Exchange exchange, String accessToken) {
        if(accessToken != null) {
            exchange.getIn().setHeader(Constants.AUTHORIZATION_HEADER, Constants.BEARER + accessToken);
        }
    }
}