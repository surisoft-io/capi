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
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.ssl.XnioSsl;

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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
public class WebsocketUtils {

    private static final Logger log = LoggerFactory.getLogger(WebsocketUtils.class);
    private final String capiContextPath;
    private final Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor;
    private final Optional<CapiUndertowTracer> capiUndertowTracer;
    private final boolean capiTrustStoreEnabled;
    private final String capiTrustStorePath;
    private final String capiTrustStorePassword;
    private final String capiTrustStoreEncoded;
    private XnioSsl xnioSsl;

    public WebsocketUtils(@Value("${camel.servlet.mapping.context-path}") String capiContextPath,
                          Optional<List<DefaultJWTProcessor<SecurityContext>>> defaultJWTProcessor,
                          Optional<CapiUndertowTracer> capiUndertowTracer,
                          @Value("${capi.trust.store.enabled}") boolean capiTrustStoreEnabled,
                          @Value("${capi.trust.store.path}") String capiTrustStorePath,
                          @Value("${capi.trust.store.password}") String capiTrustStorePassword,
                          @Value("${capi.trust.store.encoded}") String capiTrustStoreEncoded) {
        this.capiContextPath = capiContextPath;
        this.defaultJWTProcessor = defaultJWTProcessor;
        this.capiUndertowTracer = capiUndertowTracer;
        this.capiTrustStoreEnabled = capiTrustStoreEnabled;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.capiTrustStoreEncoded = capiTrustStoreEncoded;

        if(capiTrustStoreEnabled) {
            this.xnioSsl = createXnioSsl();
        }
    }

    public HttpHandler createClientHttpHandler(WebsocketClient webSocketClient, Service service) {
        CAPILoadBalancerProxyClient loadBalancingProxyClient = new CAPILoadBalancerProxyClient(capiUndertowTracer.orElse(null));
        webSocketClient.getMappingList().forEach((m) -> {
            String schema = service.getServiceMeta().getSchema() == null ? HttpProtocol.HTTP.getProtocol() : service.getServiceMeta().getSchema();
            if(capiTrustStoreEnabled) {
                loadBalancingProxyClient.addHost(URI.create(schema + "://" + m.getHostname() + ":" + m.getPort()), xnioSsl);
            } else {
                loadBalancingProxyClient.addHost(URI.create(schema + "://" + m.getHostname() + ":" + m.getPort()));
            }
        });
        return CAPIProxyHandler
                .builder()
                .setProxyClient(loadBalancingProxyClient)
                //.setMaxRequestTime(30000)
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

        //The path should be the same for all the nodes, so we take the first just to set the path.
        String rootContext = service.getMappingList().stream().toList().get(0).getRootContext();
        if(rootContext != null && !rootContext.isEmpty() && !rootContext.equals("/") && !rootContext.equals("*")) {
            websocketClient.setRootContext(rootContext);
        }
        String websocketContext = normalizeCapiContextPath() + service.getContext() + service.getMappingList().stream().toList().get(0).getRootContext();

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

    public XnioSsl createXnioSsl() {
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            if(capiTrustStoreEncoded != null && !capiTrustStoreEncoded.isEmpty()) {
                InputStream trusStoreInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(capiTrustStoreEncoded.getBytes()));
                trustStore.load(trusStoreInputStream, this.capiTrustStorePassword.toCharArray());
            } else {
                FileInputStream trustStoreFile = new FileInputStream(capiTrustStorePath);
                trustStore.load(trustStoreFile, capiTrustStorePassword.toCharArray());
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            Xnio xnio = Xnio.getInstance();
            OptionMap optionMap = OptionMap.EMPTY;
            return new UndertowXnioSsl(xnio, optionMap, sslContext);
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException |
                 KeyManagementException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public XnioSsl getXnioSsl() {
        return this.xnioSsl;
    }
}