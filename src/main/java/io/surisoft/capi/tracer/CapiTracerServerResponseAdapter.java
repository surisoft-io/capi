package io.surisoft.capi.tracer;

import brave.SpanCustomizer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;

public class CapiTracerServerResponseAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CapiTracerServerResponseAdapter.class);
    private final CapiTracer capiTracer;
    private final String url;
    public CapiTracerServerResponseAdapter(CapiTracer capiTracer, String url) {
        this.capiTracer = capiTracer;
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

        if(exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class) != null) {
            try {
                JWTClaimsSet jwtClaimsSet = capiTracer.getHttpUtils().authorizeRequest(exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class));
                String authorizedParty = jwtClaimsSet.getStringClaim(Constants.AUTHORIZED_PARTY);
                if(authorizedParty != null) {
                    span.tag(Constants.CAPI_EXCHANGE_REQUESTER_ID, authorizedParty);
                }
                String clientHost = jwtClaimsSet.getStringClaim("clientHost");
                if(clientHost != null) {
                    span.tag("capi.requester.host", clientHost);
                }
                String iss = jwtClaimsSet.getStringClaim("iss");
                if(iss != null) {
                    span.tag("capi.requester.token.issuer", iss);
                }
            } catch (AuthorizationException | BadJOSEException | ParseException | JOSEException | IOException e) {
                LOG.trace("No Authorization header detected, or access token invalid");
            }
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) exchange.getIn().getHeader(Constants.CAMEL_HTTP_SERVLET_REQUEST);

        if(httpServletRequest != null && httpServletRequest.getMethod() != null) {
            span.tag(Constants.CAPI_REQUEST_METHOD, httpServletRequest.getMethod());
        }

        if(httpServletRequest != null && httpServletRequest.getHeader(Constants.CONTENT_TYPE) != null) {
            span.tag(Constants.CAPI_REQUEST_CONTENT_TYPE, httpServletRequest.getHeader(Constants.CONTENT_TYPE));
        }

        if(httpServletRequest != null && httpServletRequest.getContentLength() > -1) {
            span.tag(Constants.CAPI_REQUEST_CONTENT_LENGTH, Integer.toString(httpServletRequest.getContentLength()));
        }

        // lets capture http response code for http based components
        String responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        if (responseCode != null) {
            span.tag(Constants.CAPI_SERVER_EXCHANGE_MESSAGE_RESPONSE_CODE, responseCode);
        }
    }
}