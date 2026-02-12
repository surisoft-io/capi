package io.surisoft.capi.rest;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.exception.CapiGatewayException;
import io.surisoft.capi.gateway.CAPIProxyHandler;
import io.surisoft.capi.oidc.WebsocketAuthorization;
import io.surisoft.capi.schema.HttpProtocol;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.tracer.CapiGatewayTracer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

@Component
@ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
public class RestComponentUtils {

    private static final Logger log = LoggerFactory.getLogger(RestComponentUtils.class);
    private final String capiContextPath;
    private final Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor;
    private final Optional<CapiGatewayTracer> capiGatewayTracer;
    private final boolean capiTrustStoreEnabled;
    private final String capiTrustStorePath;
    private final String capiTrustStorePassword;
    private final String capiTrustStoreEncoded;
    private final HttpClient jettyHttpClient;

    public RestComponentUtils(@Value("${camel.servlet.mapping.context-path}") String capiContextPath,
                              Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor,
                              Optional<CapiGatewayTracer> capiGatewayTracer,
                              @Value("${capi.trust.store.enabled}") boolean capiTrustStoreEnabled,
                              @Value("${capi.trust.store.path}") String capiTrustStorePath,
                              @Value("${capi.trust.store.password}") String capiTrustStorePassword,
                              @Value("${capi.trust.store.encoded}") String capiTrustStoreEncoded) {
        this.capiContextPath = capiContextPath;
        this.defaultJWTProcessor = defaultJWTProcessor;
        this.capiGatewayTracer = capiGatewayTracer;
        this.capiTrustStoreEnabled = capiTrustStoreEnabled;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.capiTrustStoreEncoded = capiTrustStoreEncoded;

        this.jettyHttpClient = createHttpClient();
        try {
            this.jettyHttpClient.start();
        } catch (Exception e) {
            log.error("Failed to start Jetty HttpClient", e);
            throw new RuntimeException(e);
        }
    }

    public Handler createClientHttpHandler(WebsocketClient webSocketClient, Service service) {
        List<URI> backends = new ArrayList<>();
        webSocketClient.getMappingList().forEach((m) -> {
            String scheme = service.getServiceMeta().getScheme() == null ? HttpProtocol.HTTP.getProtocol() : service.getServiceMeta().getScheme();
            backends.add(URI.create(scheme + "://" + m.getHostname() + ":" + m.getPort()));
        });
        try {
            CAPIProxyHandler handler = new CAPIProxyHandler(jettyHttpClient, backends, capiGatewayTracer.orElse(null));
            handler.start();
            return handler;
        } catch (Exception e) {
            log.error("Failed to start CAPIProxyHandler", e);
            throw new RuntimeException(e);
        }
    }

    public WebsocketAuthorization createWebsocketAuthorization() throws CapiGatewayException {
        if(defaultJWTProcessor.isPresent()) {
            return new WebsocketAuthorization(defaultJWTProcessor.get());
        }
        throw new CapiGatewayException("No OIDC provider enabled, consider enabling OIDC");
    }

    public String normalizePathForForwarding(WebsocketClient websocketClient, String path) {
        String pathWithoutCapiContext = path.replaceFirst(capiContextPath, "/");
        pathWithoutCapiContext = pathWithoutCapiContext.replaceAll(websocketClient.getServiceId(), "");
        if(websocketClient.getRootContext() != null && !websocketClient.getRootContext().isEmpty()) {
            return  websocketClient.getRootContext() + pathWithoutCapiContext;
        }
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
        WebsocketClient websocketClient = new WebsocketClient();

        String rootContext = service.getMappingList().stream().toList().get(0).getRootContext();
        if(rootContext != null && !rootContext.isEmpty() && !rootContext.equals("/") && !rootContext.equals("*")) {
            websocketClient.setRootContext(rootContext);
        }
        String websocketContext = normalizeCapiContextPath() + service.getContext() + service.getMappingList().stream().toList().get(0).getRootContext();

        websocketClient.setServiceId(service.getContext());
        websocketClient.setMappingList(service.getMappingList());
        websocketClient.setPath(websocketContext);
        websocketClient.setRequiresSubscription(service.getServiceMeta().isSecured());
        websocketClient.setHandler(createClientHttpHandler(websocketClient, service));
        return websocketClient;
    }

    public void removeClientFromMap(Map<String, WebsocketClient> websocketClientMap, Service service) {
        websocketClientMap.remove(service.getContext());
    }

    public String normalizeCapiContextPath() {
        String normalized = capiContextPath.replaceAll("/", "").replaceAll("\\*", "");
        return "/" + normalized;
    }

    private HttpClient createHttpClient() {
        if (capiTrustStoreEnabled) {
            SslContextFactory.Client sslContextFactory = createSslContextFactory();
            ClientConnector clientConnector = new ClientConnector();
            clientConnector.setSslContextFactory(sslContextFactory);
            return new HttpClient(new HttpClientTransportDynamic(clientConnector));
        }
        return new HttpClient();
    }

    private SslContextFactory.Client createSslContextFactory() {
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            if (capiTrustStoreEncoded != null && !capiTrustStoreEncoded.isEmpty()) {
                InputStream trustStoreInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(capiTrustStoreEncoded.getBytes()));
                trustStore.load(trustStoreInputStream, this.capiTrustStorePassword.toCharArray());
            } else {
                FileInputStream trustStoreFile = new FileInputStream(capiTrustStorePath);
                trustStore.load(trustStoreFile, capiTrustStorePassword.toCharArray());
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            SslContextFactory.Client factory = new SslContextFactory.Client();
            factory.setSslContext(sslContext);
            return factory;
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException |
                 KeyManagementException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
