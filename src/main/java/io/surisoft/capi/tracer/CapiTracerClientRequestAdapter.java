package io.surisoft.capi.tracer;

import brave.SpanCustomizer;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.util.URISupport;

import java.util.Locale;

public final class CapiTracerClientRequestAdapter {
    private final String spanName;
    private final String url;

    public CapiTracerClientRequestAdapter(Endpoint endpoint, String clientEndpoint) {
        this.spanName = URISupport.sanitizeUri(endpoint.getEndpointKey()).toLowerCase(Locale.ROOT);
        this.url = clientEndpoint;
    }

    public void onRequest(Exchange exchange, SpanCustomizer span) {
        span.name(spanName);
        span.tag(Constants.CAMEL_CLIENT_ENDPOINT_URL, url);
        span.tag(Constants.CAMEL_CLIENT_EXCHANGE_ID, exchange.getExchangeId());
    }
}