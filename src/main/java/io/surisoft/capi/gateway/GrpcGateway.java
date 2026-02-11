package io.surisoft.capi.gateway;

import io.surisoft.capi.schema.GrpcClient;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.WebsocketUtils;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.http.HttpURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.net.URI;
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
    private Server server;

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
        server = new Server();

        HttpConfiguration httpConfig = new HttpConfiguration();
        HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);
        HttpConnectionFactory h1 = new HttpConnectionFactory(httpConfig);

        ServerConnector connector;
        if (sslContext.isPresent()) {
            SslContextFactory.Server ssl = new SslContextFactory.Server();
            ssl.setSslContext(sslContext.get());
            connector = new ServerConnector(server, ssl, h1, h2c);
        } else {
            connector = new ServerConnector(server, h1, h2c);
        }
        connector.setPort(grpcPort);
        server.addConnector(connector);

        ProxyHandler proxyHandler = createTempProxyHandler();

        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                request.setAttribute(Constants.FORWARDED_PATH_ATTR, "/grpc.health.v1.Health/Check");
                return proxyHandler.handle(request, response, callback);
            }
        });

        try {
            server.start();
        } catch (Exception e) {
            log.error("Failed to start gRPC gateway", e);
        }
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                log.error("Failed to stop gRPC gateway", e);
            }
        }
    }

    private ProxyHandler createTempProxyHandler() {
        try {
            HttpClient httpClient = new HttpClient();
            if (sslContext.isPresent()) {
                SslContextFactory.Client clientSsl = new SslContextFactory.Client();
                clientSsl.setSslContext(sslContext.get());
                httpClient = new HttpClient(new org.eclipse.jetty.client.transport.HttpClientTransportDynamic(
                        new org.eclipse.jetty.io.ClientConnector() {{
                            setSslContextFactory(clientSsl);
                        }}
                ));
            }
            httpClient.start();
            HttpClient finalHttpClient = httpClient;
            CAPIProxyHandler handler = new CAPIProxyHandler(finalHttpClient, java.util.List.of(new URI("https://localhost:8443")), null);
            handler.start();
            return handler;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
