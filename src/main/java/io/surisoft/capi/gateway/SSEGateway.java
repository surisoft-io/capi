package io.surisoft.capi.gateway;

import io.surisoft.capi.exception.CapiGatewayException;
import io.surisoft.capi.oidc.SSEAuthorization;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.ErrorMessage;
import io.surisoft.capi.utils.SSEUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "capi.sse", name = "enabled", havingValue = "true")
public class SSEGateway {
    private static final Logger log = LoggerFactory.getLogger(SSEGateway.class);
    private final int port;
    private final Map<String, SSEClient> sseClients;
    private SSEAuthorization sseAuthorization;
    private final SSEUtils sseUtils;
    private final Optional<SSLContext> sslContext;
    private final List<String> accessControlAllowHeaders;
    private final Map<String, String> managedHeaders;
    private final String oauth2CookieName;
    private Server server;

    public SSEGateway(@Value("${capi.sse.server.port}") int port,
                      Map<String, SSEClient> sseClients, SSEUtils sseUtils,
                      Optional<SSLContext> sslContext,
                      @Value("${capi.gateway.cors.management.allowed-headers}") List<String> accessControlAllowHeaders,
                      @Value("${capi.oauth2.cookieName}") String oauth2CookieName) {
        this.port = port;
        this.sseClients = sseClients;
        this.sseUtils = sseUtils;
        this.sslContext = sslContext;
        this.accessControlAllowHeaders = accessControlAllowHeaders;
        this.oauth2CookieName = oauth2CookieName;

        managedHeaders = new java.util.HashMap<>(Constants.CAPI_CORS_MANAGED_HEADERS);
        managedHeaders.put("Access-Control-Allow-Headers", StringUtils.join(accessControlAllowHeaders, ","));
    }

    public void runProxy() {
        try {
            sseAuthorization = sseUtils.createSSEAuthorization();
        } catch (CapiGatewayException e) {
            log.warn(e.getMessage());
        }

        server = new Server();
        ServerConnector connector;
        if (sslContext.isPresent()) {
            SslContextFactory.Server ssl = new SslContextFactory.Server();
            ssl.setSslContext(sslContext.get());
            connector = new ServerConnector(server, ssl);
        } else {
            connector = new ServerConnector(server);
        }
        connector.setPort(port);
        server.addConnector(connector);

        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                String requestPath = Request.getPathInContext(request);
                String webClientId = sseUtils.getWebclientId(requestPath);
                SSEClient sseClient = sseClients.get(webClientId);
                if (sseClients.containsKey(webClientId)) {
                    if (request.getMethod().equals(Constants.OPTIONS_METHODS_VALUE)) {
                        List<String> localAccessControlAllowHeaders = new ArrayList<>(accessControlAllowHeaders);
                        if (oauth2CookieName != null && !oauth2CookieName.isEmpty()) {
                            localAccessControlAllowHeaders.add(oauth2CookieName);
                        }
                        response.getHeaders().put("Access-Control-Max-Age", Constants.ACCESS_CONTROL_MAX_AGE_VALUE);
                        String originHeader = request.getHeaders().get("Origin");
                        processOrigin(response, originHeader);
                        managedHeaders.forEach((k, v) -> {
                            String value = v;
                            if (k.equals(Constants.ACCESS_CONTROL_ALLOW_HEADERS)) {
                                value = StringUtils.join(localAccessControlAllowHeaders, ",");
                            }
                            response.getHeaders().put(k, value);
                        });
                        response.setStatus(HttpServletResponse.SC_ACCEPTED);
                        callback.succeeded();
                        return true;
                    } else {
                        if (sseAuthorization != null) {
                            if (sseAuthorization.isAuthorized(sseClient, request)) {
                                log.info("{} is authorized!", requestPath);
                                String forwardPath = sseUtils.normalizePathForForwarding(sseClient, requestPath);
                                request.setAttribute(Constants.FORWARDED_PATH_ATTR, forwardPath);
                                return sseClient.getHandler().handle(request, response, callback);
                            } else {
                                log.info("{} is not authorized!", requestPath);
                                response.setStatus(403);
                                callback.succeeded();
                                return true;
                            }
                        } else {
                            if (!sseClient.requiresSubscription()) {
                                log.info("{} is authorized!", requestPath);
                                String forwardPath = sseUtils.normalizePathForForwarding(sseClient, requestPath);
                                request.setAttribute(Constants.FORWARDED_PATH_ATTR, forwardPath);
                                return sseClient.getHandler().handle(request, response, callback);
                            } else {
                                log.info("{} is not authorized!", requestPath);
                                response.setStatus(403);
                                callback.succeeded();
                                return true;
                            }
                        }
                    }
                } else {
                    log.debug(ErrorMessage.IS_NOT_PRESENT, requestPath);
                    response.setStatus(Constants.NOT_FOUND_CODE);
                    callback.succeeded();
                    return true;
                }
            }
        });

        try {
            server.start();
        } catch (Exception e) {
            log.error("Failed to start SSE gateway", e);
        }
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                log.error("Failed to stop SSE gateway", e);
            }
        }
    }

    private void processOrigin(Response response, String origin) {
        if (origin != null && isValidOrigin(origin)) {
            response.getHeaders().put(Constants.ACCESS_CONTROL_ALLOW_ORIGIN, origin.replaceAll("(\r\n|\n)", ""));
        }
    }

    boolean isValidOrigin(String origin) {
        try {
            new URL(origin).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}
