package io.surisoft.capi.undertow;

import io.surisoft.capi.exception.CapiUndertowException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.tracer.CapiUndertowTracer;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.ErrorMessage;
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
    private final Optional<CapiUndertowTracer> capiUndertowTracer;

    public WebsocketGateway(@Value("${capi.websocket.server.port}") int port,
                            Map<String, WebsocketClient> webSocketClients,
                            WebsocketUtils websocketUtils,
                            Optional<SSLContext> sslContext,
                            Optional<CapiUndertowTracer> capiUndertowTracer) {
        this.port = port;
        this.webSocketClients = webSocketClients;
        this.websocketUtils = websocketUtils;
        this.sslContext = sslContext;
        this.capiUndertowTracer = capiUndertowTracer;
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
                    } else {
                        log.debug(ErrorMessage.IS_NOT_PRESENT, httpServerExchange.getRequestPath());
                        httpServerExchange.setStatusCode(Constants.NOT_FOUND_CODE);
                        httpServerExchange.endExchange();
                    }
                });
        builder.build().start();
    }
}