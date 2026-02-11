package io.surisoft.capi.gateway;

import io.surisoft.capi.tracer.CapiGatewayTracer;
import io.surisoft.capi.utils.Constants;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.proxy.ProxyHandler;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CAPIProxyHandler extends ProxyHandler {

    private static final Logger log = LoggerFactory.getLogger(CAPIProxyHandler.class);

    private final List<URI> backends;
    private final AtomicInteger counter = new AtomicInteger();
    private final CapiGatewayTracer tracer;

    public CAPIProxyHandler(HttpClient httpClient, List<URI> backends, CapiGatewayTracer tracer) {
        super();
        this.backends = List.copyOf(backends);
        this.tracer = tracer;
        setHttpClient(httpClient);
    }

    @Override
    protected HttpURI rewriteHttpURI(Request request) {
        if (backends.isEmpty()) {
            return null;
        }

        int idx = Math.abs(counter.getAndIncrement() % backends.size());
        URI backend = backends.get(idx);

        request.setAttribute("capi.selected.backend", backend);

        String forwardedPath = (String) request.getAttribute(Constants.FORWARDED_PATH_ATTR);
        String path = forwardedPath != null ? forwardedPath : Request.getPathInContext(request);

        String query = (String) request.getAttribute(Constants.SANITIZED_QUERY_ATTR);
        if (query == null) {
            query = request.getHttpURI().getQuery();
        }

        if (tracer != null) {
            tracer.capiProxyRequest(backend);
        }

        return HttpURI.build()
                .scheme(backend.getScheme())
                .host(backend.getHost())
                .port(backend.getPort())
                .path(path)
                .query(query)
                .asImmutable();
    }

    @Override
    protected void addProxyHeaders(Request clientToProxyRequest,
                                    org.eclipse.jetty.client.Request proxyToServerRequest) {
        super.addProxyHeaders(clientToProxyRequest, proxyToServerRequest);

        URI backend = (URI) clientToProxyRequest.getAttribute("capi.selected.backend");
        if (backend != null) {
            proxyToServerRequest.headers(headers -> {
                headers.put("Host", backend.getHost());
                headers.put("X-Forwarded-Proto", "https");
            });
        }
    }

    @Override
    public String toString() {
        if (backends.isEmpty()) {
            return "CAPIProxyHandler - no backends";
        }
        if (backends.size() == 1) {
            return "reverse-proxy( '" + backends.get(0) + "' )";
        }
        return "reverse-proxy( { '" + String.join("', '", backends.stream().map(URI::toString).toList()) + "' } )";
    }
}
