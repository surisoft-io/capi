package io.surisoft.capi.websocket;

import io.surisoft.capi.exception.CapiWebsocketException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.WebsocketUtils;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
public class WebsocketGateway {
    private static final Logger log = LoggerFactory.getLogger(WebsocketGateway.class);
    @Value("${capi.websocket.server.port}")
    private int port;
    @Autowired
    private Map<String, WebsocketClient> webSocketClients;
    private WebsocketAuthorization websocketAuthorization;
    @Autowired
    private WebsocketUtils websocketUtils;

    public void runProxy() {

        try {
            websocketAuthorization = websocketUtils.createWebsocketAuthorization();
        } catch (CapiWebsocketException e) {
            log.warn(e.getMessage());
        }

        Undertow undertow = Undertow.builder()
                .addHttpListener(port, Constants.WEBSOCKET_LISTENING_ADDRESS)
                .setHandler(httpServerExchange -> {
                    String requestPath = httpServerExchange.getRequestPath();
                    String serviceDefinitionPath = websocketUtils.getPathDefinition(requestPath);
                    if (webSocketClients.containsKey(serviceDefinitionPath)) {
                        if (httpServerExchange.getProtocol().equals(Constants.PROTOCOL_HTTP)) {
                            if (websocketAuthorization != null) {
                                if (websocketAuthorization.isAuthorized(webSocketClients.get(serviceDefinitionPath), httpServerExchange)) {
                                    log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setRequestURI(websocketUtils.normalizePathForForwarding(webSocketClients.get(serviceDefinitionPath), requestPath));
                                    httpServerExchange.setRelativePath(websocketUtils.normalizePathForForwarding(webSocketClients.get(serviceDefinitionPath), requestPath));
                                    webSocketClients.get(serviceDefinitionPath).getHttpHandler().handleRequest(httpServerExchange);
                                } else {
                                    log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setStatusCode(403);
                                    httpServerExchange.endExchange();
                                    webSocketClients.get(serviceDefinitionPath).getHttpHandler().handleRequest(httpServerExchange);
                                }
                            } else {
                                if (!webSocketClients.get(serviceDefinitionPath).requiresSubscription()) {
                                    log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setRequestURI(websocketUtils.normalizePathForForwarding(webSocketClients.get(serviceDefinitionPath), requestPath));
                                    httpServerExchange.setRelativePath(websocketUtils.normalizePathForForwarding(webSocketClients.get(serviceDefinitionPath), requestPath));
                                    webSocketClients.get(serviceDefinitionPath).getHttpHandler().handleRequest(httpServerExchange);
                                } else {
                                    log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setStatusCode(403);
                                    httpServerExchange.endExchange();
                                    webSocketClients.get(serviceDefinitionPath).getHttpHandler().handleRequest(httpServerExchange);
                                }
                            }
                        }
                    }
                }).build();
        undertow.start();
    }
}