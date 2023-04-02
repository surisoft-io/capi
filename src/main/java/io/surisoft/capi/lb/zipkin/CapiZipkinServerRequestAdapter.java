package io.surisoft.capi.lb.zipkin;

import brave.SpanCustomizer;
import io.surisoft.capi.lb.utils.Constants;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.util.URISupport;

import java.util.Locale;

public class CapiZipkinServerRequestAdapter {
    private final String spanName;
    private final String url;

    public CapiZipkinServerRequestAdapter(Exchange exchange) {
        Endpoint endpoint = exchange.getFromEndpoint();
        this.spanName = URISupport.sanitizeUri(endpoint.getEndpointKey()).toLowerCase(Locale.ROOT);
        this.url = URISupport.sanitizeUri(endpoint.getEndpointUri());
    }

    public void onRequest(Exchange exchange, SpanCustomizer span) {
        span.name(spanName);
        span.tag(Constants.CAMEL_SERVER_ENDPOINT_URL, url);
        span.tag(Constants.CAMEL_SERVER_EXCHANGE_ID, exchange.getExchangeId());
        span.tag(Constants.CAMEL_SERVER_EXCHANGE_PATTERN, exchange.getPattern().name());
    }
}