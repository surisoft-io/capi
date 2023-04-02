package io.surisoft.capi.lb.zipkin;

import brave.SpanCustomizer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import io.surisoft.capi.lb.exception.AuthorizationException;
import io.surisoft.capi.lb.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.text.ParseException;

public class CapiZipkinServerResponseAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CapiZipkinServerResponseAdapter.class);
    private final CapiZipkinTracer capiZipkinEventNotifier;
    private final String url;
    public CapiZipkinServerResponseAdapter(CapiZipkinTracer capiZipkinEventNotifier, Exchange exchange) {
        this.capiZipkinEventNotifier = capiZipkinEventNotifier;
        Endpoint endpoint = exchange.getFromEndpoint();
        this.url = URISupport.sanitizeUri(endpoint.getEndpointUri());
    }

    public void onResponse(Exchange exchange, SpanCustomizer span) {
        String exchangeId = exchange.getExchangeId();
        String pattern = exchange.getPattern().name();

        span.tag(Constants.CAMEL_CLIENT_ENDPOINT_URL, url);
        span.tag(Constants.CAMEL_SERVER_EXCHANGE_ID, exchangeId);
        span.tag(Constants.CAMEL_SERVER_EXCHANGE_PATTERN, pattern);

        if (exchange.getException() != null) {
            String message = ObjectHelper.isEmpty(exchange.getException().getMessage()) ? exchange.getException().getClass().getName() : exchange.getException().getMessage();
            span.tag(Constants.CAMEL_SERVER_EXCHANGE_FAILURE, message);
        }

        if(exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class) != null) {
            try {
                JWTClaimsSet jwtClaimsSet = capiZipkinEventNotifier.getHttpUtils().authorizeRequest(exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class));
                String authorizedParty = jwtClaimsSet.getStringClaim(Constants.AUTHORIZED_PARTY);
                if(authorizedParty != null) {
                    span.tag(Constants.CAPI_EXCHANGE_REQUESTER_ID, authorizedParty);
                }
            } catch (AuthorizationException | BadJOSEException | ParseException | JOSEException | IOException e) {
                LOG.trace("No Authorization header detected, or access token invalid");
            }
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) exchange.getIn().getHeader(Constants.CAMEL_HTTP_SERVLET_REQUEST);

        if(httpServletRequest.getMethod() != null) {
            span.tag(Constants.CAPI_REQUEST_METHOD, httpServletRequest.getMethod());
        }

        if(httpServletRequest.getHeader(Constants.CONTENT_TYPE) != null) {
            span.tag(Constants.CAPI_REQUEST_CONTENT_TYPE, httpServletRequest.getHeader(Constants.CONTENT_TYPE));
        }

        if(httpServletRequest.getContentLength() > -1) {
            span.tag(Constants.CAPI_REQUEST_CONTENT_LENGTH, Integer.toString(httpServletRequest.getContentLength()));
        }

        // lets capture http response code for http based components
        String responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        if (responseCode != null) {
            span.tag(Constants.CAPI_SERVER_EXCHANGE_MESSAGE_RESPONSE_CODE, responseCode);
        }
    }
}