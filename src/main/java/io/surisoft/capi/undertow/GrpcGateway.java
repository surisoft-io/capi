package io.surisoft.capi.undertow;

import io.surisoft.capi.schema.GrpcClient;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ServiceMeta;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.WebsocketUtils;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.xnio.OptionMap;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "capi.grpc", name = "enabled", havingValue = "true")
public class GrpcGateway {

    private static final Logger log = LoggerFactory.getLogger(GrpcGateway.class);
    private final int grpcPort;
    private final Optional<SSLContext> sslContext;
    private final WebsocketUtils websocketUtils;
    private final Map<String, GrpcClient> grpcClients;

    public GrpcGateway(@Value("${capi.grpc.server.port}") int grpcPort,
                       Optional<SSLContext> sslContext,
                       WebsocketUtils websocketUtils,
                       Map<String, GrpcClient> grpcClients) {
        this.grpcPort = grpcPort;
        this.sslContext = sslContext;
        this.websocketUtils = websocketUtils;
        this.grpcClients = grpcClients;

    }

    public void runProxy() {

        Undertow.Builder builder = Undertow.builder();
        if(sslContext.isPresent()) {
            builder.addHttpsListener(grpcPort, Constants.UNDERTOW_LISTENING_ADDRESS, sslContext.get());
        } else {
            builder.addHttpListener(grpcPort, Constants.UNDERTOW_LISTENING_ADDRESS);
        }

        //create temp
        ProxyHandler proxyHandler = createTempObjects();
        GrpcClient grpcClient = grpcClients.get("TEST-GRPC-CLIENT");


        builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
        builder.setHandler(httpServerExchange -> {
            httpServerExchange.setRequestURI("/grpc.health.v1.Health/Check");
            httpServerExchange.setRelativePath("/grpc.health.v1.Health/Check");
            proxyHandler.handleRequest(httpServerExchange);
        });

        builder.build().start();

    }

    private ProxyHandler createTempObjects() {
        //TEMP INFO:
        Service service = new Service();
        service.setName("local-grpc");
        ServiceMeta data = new ServiceMeta();
        LoadBalancingProxyClient proxy = null;
        try {
            proxy = new LoadBalancingProxyClient()
                    .addHost(new URI("https://localhost:8443"), null, websocketUtils.getXnioSsl(), OptionMap.create(UndertowOptions.ENABLE_HTTP2, true))
                    .setConnectionsPerThread(20);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return ProxyHandler.builder().setProxyClient(proxy).setMaxRequestTime( 30000).build();
    }
}