package io.surisoft.capi.websocket;

import io.surisoft.capi.exception.CapiWebsocketException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.utils.WebsocketUtils;
import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import io.surisoft.capi.utils.Constants;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
public class WebsocketGateway {
    private static final Logger log = LoggerFactory.getLogger(WebsocketGateway.class);

    @Value("${capi.websocket.server.port}")
    private int port;
    @Value("${capi.websocket.server.host}")
    private String host;
    @Autowired
    private Map<String, WebsocketClient> webSocketClients;
    private WebsocketAuthorization websocketAuthorization;
    @Autowired
    private WebsocketUtils websocketUtils;
    private Undertow undertow;

    public void runProxy() {
        try {
            websocketAuthorization = websocketUtils.createWebsocketAuthorization();
        } catch (CapiWebsocketException e) {
            log.warn(e.getMessage());
        }

        /*webSocketClients.forEach((key, value) -> {
            value.setHttpHandler(websocketUtils.createClientHttpHandler(value));
        });*/

        this.undertow = Undertow.builder()
                .addHttpListener(port, host)

                .setHandler(httpServerExchange -> {
                    String requestPath = httpServerExchange.getRequestPath();
                    if(webSocketClients.containsKey(requestPath)) {
                        if(httpServerExchange.getProtocol().equals(Constants.PROTOCOL_HTTP)) {
                            if(websocketAuthorization != null) {
                                if(websocketAuthorization.isAuthorized(webSocketClients.get(requestPath), httpServerExchange)) {
                                    log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setRequestURI(websocketUtils.normalizePathForForwarding(webSocketClients.get(requestPath), requestPath));
                                    httpServerExchange.setRelativePath(websocketUtils.normalizePathForForwarding(webSocketClients.get(requestPath), requestPath));
                                    webSocketClients.get(requestPath).getHttpHandler().handleRequest(httpServerExchange);
                                } else {
                                    log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setStatusCode(403);
                                    httpServerExchange.endExchange();
                                    webSocketClients.get(requestPath).getHttpHandler().handleRequest(httpServerExchange);
                                }
                            }
                        }
                    }
                }).build();
        undertow.start();
    }
}