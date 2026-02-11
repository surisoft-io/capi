package io.surisoft.capi.utils;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.exception.CapiGatewayException;
import io.surisoft.capi.gateway.CAPIProxyHandler;
import io.surisoft.capi.oidc.SSEAuthorization;
import io.surisoft.capi.schema.HttpProtocol;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.schema.Service;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "capi.sse", name = "enabled", havingValue = "true")
public class SSEUtils {

    private static final Logger log = LoggerFactory.getLogger(SSEUtils.class);
    private final String capiContextPath;
    private final Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor;
    private final HttpClient jettyHttpClient;

    public SSEUtils(@Value("${camel.servlet.mapping.context-path}") String capiContextPath,
                    Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor) {
        this.capiContextPath = capiContextPath;
        this.defaultJWTProcessor = defaultJWTProcessor;

        this.jettyHttpClient = new HttpClient();
        this.jettyHttpClient.setIdleTimeout(360000);
        try {
            this.jettyHttpClient.start();
        } catch (Exception e) {
            log.error("Failed to start Jetty HttpClient", e);
            throw new RuntimeException(e);
        }
    }

    public Handler createClientHttpHandler(SSEClient sseClient, Service service) {
        List<URI> backends = new ArrayList<>();
        sseClient.getMappingList().forEach((m) -> {
            if(m.getHostname().contains("http://") || m.getHostname().contains("https://")) {
                backends.add(URI.create(m.getHostname() + ":" + m.getPort()));
            } else {
                String schema = service.getServiceMeta().getScheme() == null ? HttpProtocol.HTTP.getProtocol() : service.getServiceMeta().getScheme();
                backends.add(URI.create(schema + "://" + m.getHostname() + ":" + m.getPort()));
            }
        });
        try {
            CAPIProxyHandler handler = new CAPIProxyHandler(jettyHttpClient, backends, null);
            handler.start();
            return handler;
        } catch (Exception e) {
            log.error("Failed to start CAPIProxyHandler for SSE", e);
            throw new RuntimeException(e);
        }
    }

    public SSEAuthorization createSSEAuthorization() throws CapiGatewayException {
        if(defaultJWTProcessor.isPresent()) {
            return new SSEAuthorization(defaultJWTProcessor.get());
        }
        throw new CapiGatewayException("No OIDC provider enabled, consider enabling OIDC");
    }

    public String normalizePathForForwarding(SSEClient sseClient, String path) {
        String pathWithoutCapiContext = path.replaceFirst(capiContextPath, "/");
        return pathWithoutCapiContext.replaceAll(sseClient.getApiId(), "");
    }

    public String normalizeBaseContextName() {
        return capiContextPath.replaceAll("/", "").replaceAll("\\*", "");
    }

    public String getPathDefinition(String originalRequest) {
        String[] pathParts = originalRequest.split("/");
        if(pathParts.length < 4) {
            return null;
        }
        if(!pathParts[1].equals(normalizeBaseContextName())) {
            return null;
        }
        return Constants.CAPI_CONTEXT + "/" + pathParts[2] + "/" + pathParts[3] + "/";
    }

    public SSEClient createSSEClient(Service service) {
        String sseContext = normalizeCapiContextPath() + service.getContext() + service.getMappingList().stream().toList().get(0).getRootContext();

        SSEClient sseClient = new SSEClient();
        sseClient.setApiId(service.getContext());
        sseClient.setMappingList(service.getMappingList());
        sseClient.setPath(sseContext);
        sseClient.setRequiresSubscription(service.getServiceMeta().isSecured());
        sseClient.setHandler(createClientHttpHandler(sseClient, service));
        if(service.getServiceMeta().getSubscriptionGroup() != null) {
            sseClient.setSubscriptionRole(service.getServiceMeta().getSubscriptionGroup());
        }
        return sseClient;
    }

    public String normalizeCapiContextPath() {
        String normalized = capiContextPath.replaceAll("/", "").replaceAll("\\*", "");
        return "/" + normalized;
    }

    public String getWebclientId(String originalRequest) {
        String[] pathParts = originalRequest.split("/");
        if(pathParts.length < 4) {
            return null;
        }
        if(!pathParts[1].equals(normalizeBaseContextName())) {
            return null;
        }
        return  "/" + pathParts[2] + "/" + pathParts[3];
    }
}
