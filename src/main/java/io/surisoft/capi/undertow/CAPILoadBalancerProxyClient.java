package io.surisoft.capi.undertow;

import io.surisoft.capi.tracer.CapiUndertowTracer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.util.HttpString;

import java.util.concurrent.TimeUnit;


public class CAPILoadBalancerProxyClient extends LoadBalancingProxyClient {

    private final CapiUndertowTracer capiUndertowTracer;
    private volatile String selectedHost;

    public CAPILoadBalancerProxyClient(CapiUndertowTracer capiUndertowTracer) {
        this.capiUndertowTracer = capiUndertowTracer;
    }

    public Host selectHost(HttpServerExchange exchange) {
        Host host = super.selectHost(exchange);
        if(host != null) {
            selectedHost = host.getUri().getHost();
            exchange.getRequestHeaders().put(HttpString.tryFromString("CapiSelectedHost"), host.getUri().getHost());
            if(capiUndertowTracer != null) {
                capiUndertowTracer.capiProxyRequest(host.getUri());
            }
            return host;
        }
        //no available hosts
        return null;
    }

    public String getSelectedHost() {
        return selectedHost;
    }
}