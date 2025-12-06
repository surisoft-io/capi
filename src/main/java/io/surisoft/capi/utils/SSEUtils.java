package io.surisoft.capi.utils;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.exception.CapiUndertowException;
import io.surisoft.capi.oidc.SSEAuthorization;
import io.surisoft.capi.schema.HttpProtocol;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.schema.Service;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "capi.sse", name = "enabled", havingValue = "true")
public class SSEUtils {

    private final String capiContextPath;
    private final Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor;

    public SSEUtils(@Value("${camel.servlet.mapping.context-path}") String capiContextPath,
                    Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor) {
        this.capiContextPath = capiContextPath;
        this.defaultJWTProcessor = defaultJWTProcessor;
    }

    public HttpHandler createClientHttpHandler(SSEClient sseClient, Service service) {
        LoadBalancingProxyClient loadBalancingProxyClient = new LoadBalancingProxyClient();
        sseClient.getMappingList().forEach((m) -> {
            if(m.getHostname().contains("http://") || m.getHostname().contains("https://")) {
                loadBalancingProxyClient.addHost(URI.create(m.getHostname() + ":" + m.getPort()));
            } else {
                String schema = service.getServiceMeta().getScheme() == null ? HttpProtocol.HTTP.getProtocol() : service.getServiceMeta().getScheme();
                loadBalancingProxyClient.addHost(URI.create(schema + "://" + m.getHostname() + ":" + m.getPort()));
            }
        });
        return ProxyHandler
                .builder()
                .setProxyClient(loadBalancingProxyClient)
                .setMaxRequestTime(360000)
                .setNext(ResponseCodeHandler.HANDLE_404)
                .build();
    }

    public SSEAuthorization createSSEAuthorization() throws CapiUndertowException {
        if(defaultJWTProcessor.isPresent()) {
            return new SSEAuthorization(defaultJWTProcessor.get());
        }
        throw new CapiUndertowException("No OIDC provider enabled, consider enabling OIDC");
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

        //The path should be the same for all the nodes, so we take the first just to set the path.
        String sseContext = normalizeCapiContextPath() + service.getContext() + service.getMappingList().stream().toList().get(0).getRootContext();

        SSEClient sseClient = new SSEClient();
        sseClient.setApiId(service.getContext());
        sseClient.setMappingList(service.getMappingList());
        sseClient.setPath(sseContext);
        sseClient.setRequiresSubscription(service.getServiceMeta().isSecured());
        sseClient.setHttpHandler(createClientHttpHandler(sseClient, service));
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