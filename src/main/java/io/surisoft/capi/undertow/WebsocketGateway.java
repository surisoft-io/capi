package io.surisoft.capi.undertow;

import io.surisoft.capi.exception.CapiUndertowException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.WebsocketUtils;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
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

    public WebsocketGateway(@Value("${capi.websocket.server.port}") int port,
                            Map<String, WebsocketClient> webSocketClients,
                            WebsocketUtils websocketUtils,
                            Optional<SSLContext> sslContext) {
        this.port = port;
        this.webSocketClients = webSocketClients;
        this.websocketUtils = websocketUtils;
        this.sslContext = sslContext;
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
                    WebsocketClient websocketClient = webSocketClients.get(webClientId);
                    if (webSocketClients.containsKey(webClientId)) {
                        if (httpServerExchange.getProtocol().equals(Constants.PROTOCOL_HTTP)) {
                            if (websocketAuthorization != null) {
                                if (websocketAuthorization.isAuthorized(websocketClient, httpServerExchange)) {
                                    log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setRequestURI(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                    httpServerExchange.setRelativePath(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                    websocketClient.getHttpHandler().handleRequest(httpServerExchange);
                                } else {
                                    log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setStatusCode(403);
                                    httpServerExchange.endExchange();
                                }
                            } else {
                                if (!websocketClient.requiresSubscription()) {
                                    log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setRequestURI(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                    httpServerExchange.setRelativePath(websocketUtils.normalizePathForForwarding(websocketClient, requestPath));
                                    websocketClient.getHttpHandler().handleRequest(httpServerExchange);
                                } else {
                                    log.info("{} is ot authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setStatusCode(403);
                                    httpServerExchange.endExchange();
                                }
                            }
                        }
                    } else {
                        log.info("{} is not present!", httpServerExchange.getRequestPath());
                        httpServerExchange.setStatusCode(404);
                        httpServerExchange.endExchange();
                    }
                });
        builder.build().start();
    }
}