package io.surisoft.capi.undertow;

import io.surisoft.capi.exception.CapiUndertowException;
import io.surisoft.capi.oidc.SSEAuthorization;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.ErrorMessage;
import io.surisoft.capi.utils.SSEUtils;
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
                    String webClientId = sseUtils.getWebclientId(requestPath);
                    SSEClient sseClient = sseClients.get(webClientId);
                    if (sseClients.containsKey(webClientId)) {
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
                            if (sseAuthorization != null) {
                                if (sseAuthorization.isAuthorized(sseClient, httpServerExchange)) {
                                    log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setRequestURI(sseUtils.normalizePathForForwarding(sseClient, requestPath));
                                    httpServerExchange.setRelativePath(sseUtils.normalizePathForForwarding(sseClient, requestPath));
                                    sseClient.getHttpHandler().handleRequest(httpServerExchange);
                                } else {
                                    log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setStatusCode(403);
                                    httpServerExchange.endExchange();
                                }
                            } else {
                                if (!sseClient.requiresSubscription()) {
                                    log.info("{} is authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setRequestURI(sseUtils.normalizePathForForwarding(sseClient, requestPath));
                                    httpServerExchange.setRelativePath(sseUtils.normalizePathForForwarding(sseClient, requestPath));
                                    sseClient.getHttpHandler().handleRequest(httpServerExchange);
                                } else {
                                    log.info("{} is not authorized!", httpServerExchange.getRequestPath());
                                    httpServerExchange.setStatusCode(403);
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