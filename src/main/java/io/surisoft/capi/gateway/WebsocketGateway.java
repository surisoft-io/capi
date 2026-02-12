package io.surisoft.capi.gateway;

import io.surisoft.capi.exception.CapiGatewayException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.HttpProtocol;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.tracer.CapiGatewayTracer;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.ErrorMessage;
import io.surisoft.capi.utils.WebsocketUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.core.client.WebSocketCoreClient;
import org.eclipse.jetty.websocket.core.server.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
public class WebsocketGateway {
    private static final Logger log = LoggerFactory.getLogger(WebsocketGateway.class);
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("capi.access");
    private final int port;
    private final Map<String, WebsocketClient> webSocketClients;
    private WebsocketAuthorization websocketAuthorization;
    private final WebsocketUtils websocketUtils;
    private final Optional<SSLContext> sslContext;
    private final Optional<CapiGatewayTracer> capiGatewayTracer;
    private final List<String> accessControlAllowHeaders;
    private final Map<String, String> managedHeaders;
    private final String oauth2CookieName;
    private Server server;

    public WebsocketGateway(@Value("${capi.websocket.server.port}") int port,
                            Map<String, WebsocketClient> webSocketClients,
                            WebsocketUtils websocketUtils,
                            Optional<SSLContext> sslContext,
                            Optional<CapiGatewayTracer> capiGatewayTracer,
                            @Value("${capi.gateway.cors.management.allowed-headers}") List<String> accessControlAllowHeaders,
                            @Value("${capi.oauth2.cookieName}") String oauth2CookieName) {
        this.port = port;
        this.webSocketClients = webSocketClients;
        this.websocketUtils = websocketUtils;
        this.sslContext = sslContext;
        this.capiGatewayTracer = capiGatewayTracer;
        this.accessControlAllowHeaders = accessControlAllowHeaders;
        this.oauth2CookieName = oauth2CookieName;

        managedHeaders = new java.util.HashMap<>(Constants.CAPI_CORS_MANAGED_HEADERS);
        managedHeaders.put("Access-Control-Allow-Headers", StringUtils.join(accessControlAllowHeaders, ","));
    }

    public void runProxy() {
        try {
            websocketAuthorization = websocketUtils.createWebsocketAuthorization();
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

        WebSocketCoreClient wsClient;
        try {
            wsClient = new WebSocketCoreClient(websocketUtils.getJettyHttpClient(), null);
            wsClient.start();
        } catch (Exception e) {
            log.error("Failed to start WebSocket core client", e);
            return;
        }

        AtomicInteger lbCounter = new AtomicInteger();
        WebSocketCoreClient finalWsClient = wsClient;

        // Regular HTTP handler for non-WebSocket requests (health, CORS, fallback)
        Handler.Abstract regularHandler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                long startNanos = System.nanoTime();
                try {
                    String requestPath = Request.getPathInContext(request);

                    // Health check
                    if (requestPath.equals(Constants.GATEWAY_HEALTH_PATH)) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        callback.succeeded();
                        return true;
                    }

                    String webClientId = websocketUtils.getWebclientId(requestPath);
                    capiGatewayTracer.ifPresent(tracer -> tracer.serverRequest(request, webClientId));
                    WebsocketClient websocketClient = webSocketClients.get(webClientId);

                    if (websocketClient != null) {
                        // CORS preflight
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
                        }

                        // WebSocket upgrade that fell through (auth failure or subscription required)
                        if (request.getHeaders().contains("Upgrade")) {
                            log.debug(ErrorMessage.IS_NOT_AUTHORIZED, requestPath);
                            response.setStatus(Constants.FORBIDDEN_CODE);
                            Content.Sink.write(response, true, "Forbidden", callback);
                            return true;
                        }

                        // Regular HTTP request - proxy it
                        String forwardPath = websocketUtils.normalizePathForForwarding(websocketClient, requestPath);
                        request.setAttribute(Constants.FORWARDED_PATH_ATTR, forwardPath);
                        return websocketClient.getHandler().handle(request, response, callback);
                    }

                    // Not found
                    log.debug(ErrorMessage.IS_NOT_PRESENT, requestPath);
                    response.setStatus(Constants.NOT_FOUND_CODE);
                    callback.succeeded();
                    return true;
                } finally {
                    long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                    String originalIp = Request.getRemoteAddr(request);
                    if (request.getHeaders().contains("X-Forwarded-For")) {
                        originalIp = request.getHeaders().get("X-Forwarded-For");
                    }
                    ACCESS_LOG.info(
                            "access",
                            net.logstash.logback.argument.StructuredArguments.keyValue("method", request.getMethod()),
                            net.logstash.logback.argument.StructuredArguments.keyValue("path", Request.getPathInContext(request)),
                            net.logstash.logback.argument.StructuredArguments.keyValue("status", response.getStatus()),
                            net.logstash.logback.argument.StructuredArguments.keyValue("duration_ms", durationMs),
                            net.logstash.logback.argument.StructuredArguments.keyValue("remote_ip", originalIp)
                    );
                }
            }
        };

        // WebSocket upgrade handler wraps the regular handler
        WebSocketUpgradeHandler wsUpgradeHandler = new WebSocketUpgradeHandler(configurator -> {
            configurator.addMapping("/*", (upgradeRequest, upgradeResponse, upgradeCallback) -> {
                String requestPath = Request.getPathInContext(upgradeRequest);
                String webClientId = websocketUtils.getWebclientId(requestPath);
                WebsocketClient websocketClient = webSocketClients.get(webClientId);

                if (websocketClient == null) {
                    log.debug("No WebSocket client found for path: {}", requestPath);
                    Response.writeError(upgradeRequest, upgradeResponse, upgradeCallback, Constants.NOT_FOUND_CODE);
                    return null;
                }

                // Authorization check
                if (websocketAuthorization != null) {
                    if (!websocketAuthorization.isAuthorized(websocketClient, upgradeRequest)) {
                        log.debug(ErrorMessage.IS_NOT_AUTHORIZED, requestPath);
                        Response.writeError(upgradeRequest, upgradeResponse, upgradeCallback, Constants.FORBIDDEN_CODE);
                        return null;
                    }
                } else if (websocketClient.requiresSubscription()) {
                    log.debug(ErrorMessage.IS_NOT_AUTHORIZED, requestPath);
                    Response.writeError(upgradeRequest, upgradeResponse, upgradeCallback, Constants.FORBIDDEN_CODE);
                    return null;
                }

                log.info("WebSocket upgrade authorized for path: {}", requestPath);
                capiGatewayTracer.ifPresent(tracer -> tracer.serverRequest(upgradeRequest, webClientId));

                // Select backend via round-robin
                URI backend = selectBackend(websocketClient, lbCounter);
                if (backend == null) {
                    log.error("No backend available for WebSocket client: {}", webClientId);
                    Response.writeError(upgradeRequest, upgradeResponse, upgradeCallback, 502);
                    return null;
                }

                capiGatewayTracer.ifPresent(tracer -> tracer.capiProxyRequest(backend));

                // Build target URI with forwarded path
                String forwardPath = websocketUtils.normalizePathForForwarding(websocketClient, requestPath);
                String wsScheme = "https".equals(backend.getScheme()) ? "wss" : "ws";
                URI targetUri = URI.create(wsScheme + "://" + backend.getHost() + ":" + backend.getPort() + forwardPath);
                log.info("Proxying WebSocket to: {}", targetUri);

                return new CAPIWebSocketProxy(targetUri, finalWsClient);
            });

            configurator.setHandler(regularHandler);
        });

        server.setHandler(wsUpgradeHandler);

        try {
            server.start();
            log.info("WebSocket gateway started on port {}", port);
        } catch (Exception e) {
            log.error("Failed to start WebSocket gateway", e);
        }
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                log.error("Failed to stop WebSocket gateway", e);
            }
        }
    }

    URI selectBackend(WebsocketClient websocketClient, AtomicInteger counter) {
        List<URI> backends = new ArrayList<>();
        websocketClient.getMappingList().forEach(m -> {
            String scheme = HttpProtocol.HTTP.getProtocol();
            if (m.getHostname().contains("http://") || m.getHostname().contains("https://")) {
                backends.add(URI.create(m.getHostname() + ":" + m.getPort()));
            } else {
                backends.add(URI.create(scheme + "://" + m.getHostname() + ":" + m.getPort()));
            }
        });
        if (backends.isEmpty()) {
            return null;
        }
        int idx = Math.abs(counter.getAndIncrement() % backends.size());
        return backends.get(idx);
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
