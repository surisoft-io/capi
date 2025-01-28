package io.surisoft.capi.undertow;

import io.surisoft.capi.tracer.CapiUndertowTracer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;


public class CAPILoadBalancerProxyClient extends LoadBalancingProxyClient {

    private final CapiUndertowTracer capiUndertowTracer;

    public CAPILoadBalancerProxyClient(CapiUndertowTracer capiUndertowTracer) {
        this.capiUndertowTracer = capiUndertowTracer;
    }

    public Host selectHost(HttpServerExchange exchange) {
        Host host = super.selectHost(exchange);
        if(host != null) {
            if(capiUndertowTracer != null) {
                capiUndertowTracer.capiProxyRequest(host.getUri());
            }
            return host;
        }
        //no available hosts
        return null;
    }
}