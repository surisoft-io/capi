package io.surisoft.capi.tracer;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.opentelemetry.api.trace.Span;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.util.URISupport;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CapiTracerServerRequestAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CapiTracerServerRequestAdapter.class);
    private final String spanName;
    private final String url;
    private final CapiTracer capiTracer;
    private final Cache<String, Service> serviceCache;

    public CapiTracerServerRequestAdapter(Exchange exchange, CapiTracer capiTracer, Cache<String, Service> serviceCache) {
        Endpoint endpoint = exchange.getFromEndpoint();
        this.spanName = URISupport.sanitizeUri(endpoint.getEndpointKey()).toLowerCase(Locale.ROOT);
        this.url = exchange.getIn().getHeader("CamelHttpUri", String.class);
        this.capiTracer = capiTracer;
        this.serviceCache = serviceCache;
    }

    public void onRequest(Exchange exchange, Span span) {
        try {
            String accessToken;
            try {
                accessToken = capiTracer.getHttpUtils().processAuthorizationAccessToken(exchange);
                if(accessToken != null) {
                    SignedJWT signedJWT = SignedJWT.parse(accessToken);
                    JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                    Date expirationTime = jwtClaimsSet.getExpirationTime();
                    if(expirationTime.before(Calendar.getInstance().getTime())) {
                        span.setAttribute(Constants.CAPI_TOKEN_EXPIRED, Boolean.toString(true));
                    } else {
                        span.setAttribute(Constants.CAPI_TOKEN_EXPIRED, Boolean.toString(false));
                    }
                    String authorizedParty = jwtClaimsSet.getStringClaim(Constants.AUTHORIZED_PARTY);
                    if(authorizedParty != null) {
                        span.setAttribute(Constants.CAPI_EXCHANGE_REQUESTER_ID, authorizedParty);
                    }
                    String clientHost = jwtClaimsSet.getStringClaim("clientHost");
                    if(clientHost != null) {
                        span.setAttribute("capi.requester.host", clientHost);
                    }
                    String iss = jwtClaimsSet.getStringClaim("iss");
                    if(iss != null) {
                        span.setAttribute(Constants.CAPI_REQUESTER_TOKEN_ISSUER, iss);
                    }
                }
            } catch (AuthorizationException e) {
                LOG.trace("No Authorization header detected, or access token invalid");
            }
        } catch (ParseException e) {
            LOG.trace("No Authorization header detected, or access token invalid");
        }

        if(exchange.getIn().getHeader("Content-Type") != null) {
            span.setAttribute("capi.incoming.request.content.type", exchange.getIn().getHeader("Content-Type", String.class));
        }

        if(exchange.getIn().getHeader("X-Forwarded-For") != null) {
            span.setAttribute("capi.incoming.request.original.ip", exchange.getIn().getHeader("X-Forwarded-For", String.class));
        }

        try {
            String serviceId = url.trim().split("/")[2] + ":" + url.split("/")[3];
            Service service = serviceCache.peek(serviceId);
            if(service != null) {
                service.getServiceMeta().getExtraServiceMeta().forEach(span::setAttribute);
            }
        } catch (Exception e) {
            LOG.trace("Problem parsing cached service metadata");
        }


        span.setAttribute("capi-instance", capiTracer.getCapiNamespace());
        span.updateName(spanName);
        span.setAttribute(Constants.CAMEL_SERVER_ENDPOINT_URL, url);
        span.setAttribute(Constants.CAMEL_SERVER_EXCHANGE_ID, exchange.getExchangeId());
    }
}