package io.surisoft.capi.undertow;

import io.surisoft.capi.exception.CapiUndertowException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.tracer.CapiUndertowTracer;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.ErrorMessage;
import io.surisoft.capi.utils.WebsocketUtils;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
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
@ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
public class WebsocketGateway {
    private static final Logger log = LoggerFactory.getLogger(WebsocketGateway.class);
    private final int port;
    private final Map<String, WebsocketClient> webSocketClients;
    private WebsocketAuthorization websocketAuthorization;
    private final WebsocketUtils websocketUtils;
    private final Optional<SSLContext> sslContext;
    private final Optional<CapiUndertowTracer> capiUndertowTracer;
    private final List<String> accessControlAllowHeaders;
    private final Map<String, String> managedHeaders;
    private final String oauth2CookieName;

    public WebsocketGateway(@Value("${capi.websocket.server.port}") int port,
                            Map<String, WebsocketClient> webSocketClients,
                            WebsocketUtils websocketUtils,
                            Optional<SSLContext> sslContext,
                            Optional<CapiUndertowTracer> capiUndertowTracer,
                            @Value("${capi.gateway.cors.management.allowed-headers}") List<String> accessControlAllowHeaders,
                            @Value("${capi.oauth2.cookieName}") String oauth2CookieName) {
        this.port = port;
        this.webSocketClients = webSocketClients;
        this.websocketUtils = websocketUtils;
        this.sslContext = sslContext;
        this.capiUndertowTracer = capiUndertowTracer;
        this.accessControlAllowHeaders = accessControlAllowHeaders;
        this.oauth2CookieName = oauth2CookieName;

        managedHeaders = new java.util.HashMap<>(Constants.CAPI_CORS_MANAGED_HEADERS);
        managedHeaders.put("Access-Control-Allow-Headers", StringUtils.join(accessControlAllowHeaders, ","));

    }

    public void runProxy() {

        try {
            websocketAuthorization = websocketUtils.createWebsocketAuthorization();
        } catch (CapiUndertowException e) {
            log.warn(e.getMessage());
        }

        Undertow.Builder builder = Undertow.builder();

        if(sslContext.isPresent()) {
            builder.addHttpsListener(port, Constants.UNDERTOW_LISTENING_ADDRESS, sslContext.get());
        } else {
            builder.addHttpListener(port, Constants.UNDERTOW_LISTENING_ADDRESS);
        }

        builder
                .setHandler(httpServerExchange -> {
                    String requestPath = httpServerExchange.getRequestPath();
                    String webClientId = websocketUtils.getWebclientId(requestPath);
                    capiUndertowTracer.ifPresent(undertowTracer -> undertowTracer.serverRequest(httpServerExchange, webClientId));
                    WebsocketClient websocketClient = webSocketClients.get(webClientId);
                    if (webSocketClients.containsKey(webClientId)) {
                        if(httpServerExchange.getRequestMethod().equals(HttpString.tryFromString(Constants.OPTIONS_METHODS_VALUE))) {
                            List<String> localAccessControlAllowHeaders = new ArrayList<>(accessControlAllowHeaders);
                            if(oauth2CookieName != null && !oauth2CookieName.isEmpty()) {
                                localAccessControlAllowHeaders.add(oauth2CookieName);
                            }
                            httpServerExchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Max-Age"), Constants.ACCESS_CONTROL_MAX_AGE_VALUE);
                            HeaderValues originHeader = httpServerExchange.getRequestHeaders().get("Origin");
                            processOrigin(httpServerExchange, originHeader.get(0));
                            managedHeaders.forEach((k, v) -> {
                                if(k.equals(Constants.ACCESS_CONTROL_ALLOW_HEADERS)) {
                                    v = StringUtils.join(localAccessControlAllowHeaders, ",");
                                }
                                httpServerExchange.getResponseHeaders().put(HttpString.tryFromString(k), v);
                            });
                            httpServerExchange.setStatusCode(HttpServletResponse.SC_ACCEPTED);
                            httpServerExchange.endExchange();
                        } else {
                            if (httpServerExchange.getProtocol().equals(Constants.PROTOCOL_HTTP)) {
                                if (websocketAuthorization != null) {
                                    if (websocketAuthorization.isAuthorized(websocketClient, httpServerExchange)) {
                                        log.debug(ErrorMessage.IS_AUTHORIZED, httpServerExchange.getRequestPath());
                                        httpServerExchange.setRequestURI(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                        httpServerExchange.setRelativePath(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                        websocketClient.getHttpHandler().handleRequest(httpServerExchange);
                                    } else {
                                        log.debug(ErrorMessage.IS_NOT_AUTHORIZED, httpServerExchange.getRequestPath());
                                        httpServerExchange.setStatusCode(Constants.FORBIDDEN_CODE);
                                        httpServerExchange.endExchange();
                                    }
                                } else {
                                    if (!websocketClient.requiresSubscription()) {
                                        log.debug(ErrorMessage.IS_AUTHORIZED, httpServerExchange.getRequestPath());
                                        httpServerExchange.setRequestURI(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                        httpServerExchange.setRelativePath(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                        websocketClient.getHttpHandler().handleRequest(httpServerExchange);
                                    } else {
                                        log.debug(ErrorMessage.IS_NOT_AUTHORIZED, httpServerExchange.getRequestPath());
                                        httpServerExchange.setStatusCode(Constants.FORBIDDEN_CODE);
                                        httpServerExchange.endExchange();
                                    }
                                }
                            }
                        }
                    } else {
                        log.debug(ErrorMessage.IS_NOT_PRESENT, httpServerExchange.getRequestPath());
                        httpServerExchange.setStatusCode(Constants.NOT_FOUND_CODE);
                        httpServerExchange.endExchange();
                    }
                });
        builder.build().start();
    }

    private void processOrigin(HttpServerExchange request, String origin) {
        if(isValidOrigin(origin)) {
            request.getResponseHeaders().put(HttpString.tryFromString(Constants.ACCESS_CONTROL_ALLOW_ORIGIN), origin.replaceAll("(\r\n|\n)", ""));
        }
    }

    private boolean isValidOrigin(String origin) {
        try {
            new URL(origin).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}