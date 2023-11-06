package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class OpenApiProcessor implements Processor {
    private final OpenAPI openAPI;
    public OpenApiProcessor(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String callingPath = (String) exchange.getIn().getHeader("CamelHttpPath");
        String callingMethod = (String) exchange.getIn().getHeader("CamelHttpMethod");
        if(!validateRequest(callingPath, callingMethod)) {
            sendException(exchange);
        }
    }

    public boolean validateRequest(String requestPath, String httpMethod) {
        for (String path : openAPI.getPaths().keySet()) {
            // Check if the requestPath matches any of the defined paths
            if (isPathMatch(requestPath, path)) {
                Operation operation = switch (httpMethod.toLowerCase()) {
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
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPathMatch(String requestPath, String definedPath) {
        // Normalize path by removing leading/trailing slashes
        requestPath = requestPath.replaceAll("^/+", "").replaceAll("/+$", "");
        definedPath = definedPath.replaceAll("^/+", "").replaceAll("/+$", "");

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

    private void sendException(Exchange exchange) {
        String message = "Invalid service call";
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, message);
        exchange.setException(new AuthorizationException(message));
    }
}