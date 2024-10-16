package io.surisoft.capi.utils;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.exception.CapiUndertowException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.HttpProtocol;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.tracer.CapiUndertowTracer;
import io.surisoft.capi.undertow.CAPILoadBalancerProxyClient;
import io.surisoft.capi.undertow.CAPIProxyHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class WebsocketUtils {

    private final String capiContextPath;
    private final Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor;
    private final Optional<CapiUndertowTracer> capiUndertowTracer;

    public WebsocketUtils(@Value("${camel.servlet.mapping.context-path}") String capiContextPath,
                          Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor,
                          Optional<CapiUndertowTracer> capiUndertowTracer) {
        this.capiContextPath = capiContextPath;
        this.defaultJWTProcessor = defaultJWTProcessor;
        this.capiUndertowTracer = capiUndertowTracer;
    }

    public HttpHandler createClientHttpHandler(WebsocketClient webSocketClient, Service service) {
        CAPILoadBalancerProxyClient loadBalancingProxyClient = new CAPILoadBalancerProxyClient(capiUndertowTracer.orElse(null));

        webSocketClient.getMappingList().forEach((m) -> {
            String schema = service.getServiceMeta().getSchema() == null ? HttpProtocol.HTTP.getProtocol() : service.getServiceMeta().getSchema();
            loadBalancingProxyClient.addHost(URI.create(schema + "://" + m.getHostname() + ":" + m.getPort()));
        });
        return CAPIProxyHandler
                .builder()
                .setProxyClient(loadBalancingProxyClient)
                .setMaxRequestTime(30000)
                .setNext(ResponseCodeHandler.HANDLE_404)
                .build();
    }

    public WebsocketAuthorization createWebsocketAuthorization() throws CapiUndertowException {
        if(defaultJWTProcessor.isPresent()) {
            return new WebsocketAuthorization(defaultJWTProcessor.get());
        }
        throw new CapiUndertowException("No OIDC provider enabled, consider enabling OIDC");
    }

    public String normalizePathForForwarding(WebsocketClient websocketClient, String path) {
        String pathWithoutCapiContext = path.replaceAll(normalizeCapiContextPath(), "");
        return pathWithoutCapiContext.replaceAll(websocketClient.getServiceId(), "");
    }

    public String normalizeBaseContextName() {
        return capiContextPath.replaceAll("/", "").replaceAll("\\*", "");
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

    public WebsocketClient createWebsocketClient(Service service) {

        //The path should be the same for all the nodes, so we take the first just to set the path.
        String websocketContext = normalizeCapiContextPath() + service.getContext() + service.getMappingList().stream().toList().get(0).getRootContext();

        WebsocketClient websocketClient = new WebsocketClient();

        websocketClient.setServiceId(service.getContext());
        websocketClient.setMappingList(service.getMappingList());
        websocketClient.setPath(websocketContext);
        websocketClient.setRequiresSubscription(service.getServiceMeta().isSecured());
        websocketClient.setHttpHandler(createClientHttpHandler(websocketClient, service));
        return websocketClient;
    }

    public void removeClientFromMap(Map<String, WebsocketClient> websocketClientMap, Service service) {
        websocketClientMap.remove(service.getContext());
    }

    public String normalizeCapiContextPath() {
        String normalized = capiContextPath.replaceAll("/", "").replaceAll("\\*", "");
        return "/" + normalized;
    }
}