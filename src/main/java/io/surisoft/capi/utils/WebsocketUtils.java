package io.surisoft.capi.utils;

import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.exception.CapiWebsocketException;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.HttpProtocol;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class WebsocketUtils {
    @Autowired(required = false)
    private DefaultJWTProcessor defaultJWTProcessor;

    public HttpHandler createClientHttpHandler(WebsocketClient webSocketClient, Service service) {
        LoadBalancingProxyClient loadBalancingProxyClient = new LoadBalancingProxyClient();

        webSocketClient.getMappingList().forEach((m) -> {
            String schema = service.getServiceMeta().getSchema() == null ? HttpProtocol.HTTP.getProtocol() : service.getServiceMeta().getSchema();
            loadBalancingProxyClient.addHost(URI.create(schema + "://" + m.getHostname() + ":" + m.getPort()));
        });
        return ProxyHandler
                .builder()
                .setProxyClient(loadBalancingProxyClient)
                .setMaxRequestTime(30000)
                .setNext(ResponseCodeHandler.HANDLE_404)
                .build();
    }

    public WebsocketAuthorization createWebsocketAuthorization() throws CapiWebsocketException {
        if(defaultJWTProcessor != null) {
            return new WebsocketAuthorization(defaultJWTProcessor);
        }
        throw new CapiWebsocketException("No OIDC provider enabled, consider enabling OIDC");
    }

    public String normalizePathForForwarding(WebsocketClient websocketClient, String path) {
        String pathWithoutCapiContext = path.replaceAll(Constants.CAPI_CONTEXT, "");
        return pathWithoutCapiContext.replaceAll(websocketClient.getApiId(), "");
    }

    public WebsocketClient createWebsocketClient(Service service) {

        //The path should be the same for all the nodes, so we take the first just to set the path.
        String websocketContext = Constants.CAPI_CONTEXT + service.getContext() + service.getMappingList().stream().toList().get(0).getRootContext();

        WebsocketClient websocketClient = new WebsocketClient();

        websocketClient.setApiId(service.getContext());
        websocketClient.setMappingList(service.getMappingList());
        websocketClient.setPath(websocketContext);
        websocketClient.setRequiresSubscription(service.getServiceMeta().isSecured());
        websocketClient.setHttpHandler(createClientHttpHandler(websocketClient, service));
        return websocketClient;
    }
}