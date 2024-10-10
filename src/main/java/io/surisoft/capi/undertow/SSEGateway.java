package io.surisoft.capi.undertow;

import io.surisoft.capi.exception.CapiUndertowException;
import io.surisoft.capi.oidc.SSEAuthorization;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.SSEUtils;
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
@ConditionalOnProperty(prefix = "capi.sse", name = "enabled", havingValue = "true")
public class SSEGateway {
    private static final Logger log = LoggerFactory.getLogger(SSEGateway.class);
    private final int port;
    private final Map<String, SSEClient> sseClients;
    private SSEAuthorization sseAuthorization;
    private final SSEUtils sseUtils;
    private final Optional<SSLContext> sslContext;

    public SSEGateway(@Value("${capi.sse.server.port}") int port, Map<String, SSEClient> sseClients, SSEUtils sseUtils, Optional<SSLContext> sslContext) {
        this.port = port;
        this.sseClients = sseClients;
        this.sseUtils = sseUtils;
        this.sslContext = sslContext;
    }

    public void runProxy() {
        try {
            sseAuthorization = sseUtils.createSSEAuthorization();
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
                    String serviceDefinitionPath = sseUtils.getPathDefinition(requestPath);
                    if (sseClients.containsKey(serviceDefinitionPath)) {
                        if (sseAuthorization != null) {
                            if (sseAuthorization.isAuthorized(sseClients.get(serviceDefinitionPath), httpServerExchange)) {
                                log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                httpServerExchange.setRequestURI(sseUtils.normalizePathForForwarding(sseClients.get(serviceDefinitionPath), requestPath));
                                httpServerExchange.setRelativePath(sseUtils.normalizePathForForwarding(sseClients.get(serviceDefinitionPath), requestPath));
                                sseClients.get(serviceDefinitionPath).getHttpHandler().handleRequest(httpServerExchange);
                            } else {
                                log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                httpServerExchange.setStatusCode(403);
                                httpServerExchange.endExchange();
                            }
                        } else {
                            if (!sseClients.get(serviceDefinitionPath).requiresSubscription()) {
                                log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                httpServerExchange.setRequestURI(sseUtils.normalizePathForForwarding(sseClients.get(serviceDefinitionPath), requestPath));
                                httpServerExchange.setRelativePath(sseUtils.normalizePathForForwarding(sseClients.get(serviceDefinitionPath), requestPath));
                                sseClients.get(serviceDefinitionPath).getHttpHandler().handleRequest(httpServerExchange);
                            } else {
                                log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                httpServerExchange.setStatusCode(403);
                                httpServerExchange.endExchange();
                            }
                        }
                    }
                });
        builder.build().start();
    }
}