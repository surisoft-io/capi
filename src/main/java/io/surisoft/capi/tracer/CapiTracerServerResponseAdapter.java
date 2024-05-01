package io.surisoft.capi.tracer;

import brave.SpanCustomizer;
import io.surisoft.capi.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;

public class CapiTracerServerResponseAdapter {
    private final String url;
    public CapiTracerServerResponseAdapter(String url) {
        this.url = url;
    }

    public void onResponse(Exchange exchange, SpanCustomizer span) {
        String exchangeId = exchange.getExchangeId();

        span.tag(Constants.CAMEL_CLIENT_ENDPOINT_URL, url);
        span.tag(Constants.CAMEL_SERVER_EXCHANGE_ID, exchangeId);

        if (exchange.getException() != null) {
            String message = ObjectHelper.isEmpty(exchange.getException().getMessage()) ? exchange.getException().getClass().getName() : exchange.getException().getMessage();
            span.tag(Constants.CAMEL_SERVER_EXCHANGE_FAILURE, message);
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) exchange.getIn().getHeader(Constants.CAMEL_HTTP_SERVLET_REQUEST);

        if(httpServletRequest != null && httpServletRequest.getMethod() != null) {
            span.tag(Constants.CAPI_REQUEST_METHOD, httpServletRequest.getMethod());
            span.name(httpServletRequest.getMethod());
        }

        if(httpServletRequest != null && httpServletRequest.getHeader(Constants.CONTENT_TYPE) != null) {
            span.tag(Constants.CAPI_REQUEST_CONTENT_TYPE, httpServletRequest.getHeader(Constants.CONTENT_TYPE));
        }

        if(httpServletRequest != null && httpServletRequest.getContentLength() > -1) {
            span.tag(Constants.CAPI_REQUEST_CONTENT_LENGTH, Integer.toString(httpServletRequest.getContentLength()));
        }

        if(exchange.getIn().getHeader(Constants.REASON_MESSAGE_HEADER) != null) {
            span.tag(Constants.CAPI_REQUEST_ERROR_MESSAGE, (String) exchange.getIn().getHeader(Constants.REASON_MESSAGE_HEADER));
        }

        String responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        if (responseCode != null) {
            span.tag(Constants.CAPI_SERVER_EXCHANGE_MESSAGE_RESPONSE_CODE, responseCode);
        }

        long clientStart = (Long) exchange.getProperty(Constants.CLIENT_START_TIME);
        long clientEnd = (Long) exchange.getProperty(Constants.CLIENT_END_TIME);
        double clientResponseTime = clientEnd - clientStart;
        String clientEndpoint = (String) exchange.getProperty(Constants.CLIENT_ENDPOINT);
        span.tag("capi.client.response.time", clientResponseTime+"ms");
        span.tag("capi.client.endpoint", (clientEndpoint.contains("/capi-error") ? "-" : clientEndpoint));
        span.tag("capi.client.response.code", (String) exchange.getProperty(Constants.CLIENT_RESPONSE_CODE));
    }
}