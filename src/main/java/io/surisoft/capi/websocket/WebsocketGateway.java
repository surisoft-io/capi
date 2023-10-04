package io.surisoft.capi.websocket;

import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.surisoft.capi.exception.CapiWebsocketException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.HttpProtocol;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.utils.WebsocketUtils;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.LearningPushHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.session.SessionAttachmentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import io.surisoft.capi.utils.Constants;
import org.xnio.OptionMap;
import org.xnio.Xnio;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;

import static io.undertow.Handlers.predicate;
import static io.undertow.Handlers.resource;
import static io.undertow.predicate.Predicates.secure;

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
    private Undertow undertowGrpc;

    public void runProxy() {
        try {
            websocketAuthorization = websocketUtils.createWebsocketAuthorization();
        } catch (CapiWebsocketException e) {
            log.warn(e.getMessage());
        }

        /*webSocketClients.forEach((key, value) -> {
            value.setHttpHandler(websocketUtils.createClientHttpHandler(value));
        });*/

        undertowGrpc = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpListener(8382, host)
                .setHandler(ProxyHandler.builder().setProxyClient(proxy()).setMaxRequestTime( 30000).build())
                .build();
        /*reverseProxy.start();
        this.undertowGrpc = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpListener(8382, host)
                .setHandler(httpServerExchange -> {
                    String requestPath = httpServerExchange.getRequestPath();
                    log.info(httpServerExchange.getProtocol().toString());
                    delete().handleRequest(httpServerExchange);

                }).build();*/
        this.undertowGrpc.start();

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

    private LoadBalancingProxyClient proxy() {
        LoadBalancingProxyClient loadBalancingProxyClient = new LoadBalancingProxyClient();
        loadBalancingProxyClient.addHost(URI.create("http://localhost:8080"));

        return loadBalancingProxyClient;
    }


}