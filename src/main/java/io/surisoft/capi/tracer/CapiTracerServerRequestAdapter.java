package io.surisoft.capi.tracer;

import brave.SpanCustomizer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CapiTracerServerRequestAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CapiTracerServerRequestAdapter.class);
    private final String spanName;
    private final String url;
    private final CapiTracer capiTracer;

    public CapiTracerServerRequestAdapter(Exchange exchange, String serviceName, CapiTracer capiTracer) {
        Endpoint endpoint = exchange.getFromEndpoint();
        this.spanName = URISupport.sanitizeUri(endpoint.getEndpointKey()).toLowerCase(Locale.ROOT);
        this.url = serviceName;
        this.capiTracer = capiTracer;
    }

    public void onRequest(Exchange exchange, SpanCustomizer span) {
        try {
            String accessToken;
            try {
                accessToken = capiTracer.getHttpUtils().processAuthorizationAccessToken(exchange);
                if(accessToken != null) {
                    SignedJWT signedJWT = SignedJWT.parse(accessToken);
                    JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                    Date expirationTime = jwtClaimsSet.getExpirationTime();
                    if(expirationTime.before(Calendar.getInstance().getTime())) {
                        span.tag(Constants.CAPI_TOKEN_EXPIRED, Boolean.toString(true));
                    } else {
                        span.tag(Constants.CAPI_TOKEN_EXPIRED, Boolean.toString(false));
                    }
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
                }
            } catch (AuthorizationException e) {
                LOG.trace("No Authorization header detected, or access token invalid");
            }
        } catch (ParseException e) {
            LOG.trace("No Authorization header detected, or access token invalid");
        }

        span.name(spanName);
        span.tag(Constants.CAMEL_SERVER_ENDPOINT_URL, url);
        span.tag(Constants.CAMEL_SERVER_EXCHANGE_ID, exchange.getExchangeId());
    }
}