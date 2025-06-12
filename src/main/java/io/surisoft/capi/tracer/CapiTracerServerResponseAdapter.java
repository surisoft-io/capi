package io.surisoft.capi.tracer;

import brave.SpanCustomizer;
import io.surisoft.capi.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapiTracerServerResponseAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CapiTracerServerResponseAdapter.class);
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

        if(exchange.getProperties().containsKey(Constants.CAPI_META_THROTTLE_DURATION)) {
            span.tag(Constants.CAPI_META_THROTTLE_DURATION, exchange.getProperty(Constants.CAPI_META_THROTTLE_DURATION, String.class));
        }

        if(exchange.getProperties().containsKey(Constants.CAPI_META_THROTTLE_TOTAL_CALLS_ALLOWED)) {
            span.tag(Constants.CAPI_META_THROTTLE_TOTAL_CALLS_ALLOWED, exchange.getProperty(Constants.CAPI_META_THROTTLE_TOTAL_CALLS_ALLOWED, String.class));
        }

        if(exchange.getProperties().containsKey(Constants.CAPI_META_THROTTLE_CURRENT_CALL_NUMBER)) {
            span.tag(Constants.CAPI_META_THROTTLE_CURRENT_CALL_NUMBER, exchange.getProperty(Constants.CAPI_META_THROTTLE_CURRENT_CALL_NUMBER, String.class));
        }

        if(exchange.getProperties().containsKey(Constants.REASON_MESSAGE_HEADER)) {
            span.tag(Constants.REASON_MESSAGE_HEADER, exchange.getProperty(Constants.REASON_MESSAGE_HEADER, String.class));
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

        Long clientResponseTime = getClientResponseTime(exchange);
        if(clientResponseTime != null) {
            span.tag("capi.client.response.time", clientResponseTime + "");
        }

        String clientEndpoint = exchange.getProperty(Constants.CLIENT_ENDPOINT, String.class);
        if(clientEndpoint != null) {
            span.tag("capi.client.address", clientEndpoint);
        }

        String clientResponseCode = exchange.getProperty(Constants.CLIENT_RESPONSE_CODE, String.class);
        if(clientResponseCode != null) {
            span.tag("capi.client.response.code", clientResponseCode);
        }
    }

    private Long getClientResponseTime(Exchange exchange) {
        Long clientStartTime = exchange.getProperty(Constants.CLIENT_START_TIME, Long.class);
        Long clientEndTime = exchange.getProperty(Constants.CLIENT_END_TIME, Long.class);
        if(clientStartTime != null && clientEndTime != null) {
            return (clientEndTime - clientStartTime);
        } else {
            return null;
        }
    }
}