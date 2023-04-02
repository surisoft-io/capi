package io.surisoft.capi.lb.zipkin;

import brave.SpanCustomizer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import io.surisoft.capi.lb.exception.AuthorizationException;
import io.surisoft.capi.lb.utils.Constants;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.StreamCache;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;

import static org.apache.camel.zipkin.ZipkinHelper.prepareBodyForLogging;

public class CapiZipkinClientResponseAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CapiZipkinClientResponseAdapter.class);
    private final CapiZipkinTracer eventNotifier;
    private final String url;

    public CapiZipkinClientResponseAdapter(CapiZipkinTracer eventNotifier, Endpoint endpoint) {
        this.eventNotifier = eventNotifier;
        this.url = URISupport.sanitizeUri(endpoint.getEndpointUri());
    }

    void onResponse(Exchange exchange, SpanCustomizer span) {
        span.tag("camel.client.endpoint.url", normalizeCamelUrl(url));
        span.tag("camel.client.exchange.id", exchange.getExchangeId());

        if(exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class) != null) {
            try {
                JWTClaimsSet jwtClaimsSet = eventNotifier.getHttpUtils().authorizeRequest(exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class));
                String authorizedParty = jwtClaimsSet.getStringClaim("azp");
                if(authorizedParty != null) {
                    span.tag("capi.exchange.requester.id", authorizedParty);
                }
            } catch (AuthorizationException | BadJOSEException | ParseException | JOSEException | IOException e) {
                LOG.trace("No Authorization header detected, or access token invalid");
            }
        }

        if(exchange.getIn().getHeader("tenant", String.class) != null) {
            span.tag("capi.tenant.id", exchange.getIn().getHeader("tenant", String.class));
        }

        String apiId = "";
        if(exchange.getFromRouteId().startsWith("rd_")) {
            String removedRestDefinitionPrefix = exchange.getFromRouteId().replaceAll("rd_", "");
            apiId = removedRestDefinitionPrefix.substring(0, removedRestDefinitionPrefix.lastIndexOf(":"));
            span.tag("capi.exchange.message.api.id", apiId);
        }

        if (eventNotifier.isIncludeMessageBody() || eventNotifier.isIncludeMessageBodyStreams()) {
            boolean streams = eventNotifier.isIncludeMessageBodyStreams();
            StreamCache cache = prepareBodyForLogging(exchange, streams);
            String body = MessageHelper.extractBodyForLogging(exchange.getMessage(), "", streams, streams);
            span.tag("capi.exchange.message.response.body.size", Integer.toString(body.getBytes().length));
            if (cache != null) {
                cache.reset();
            }
        }

        // lets capture http response code for http based components
        String responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);
        if (responseCode != null) {
            span.tag("camel.client.exchange.message.response.code", responseCode);
        }
    }

    private String normalizeCamelUrl(String url) {
        if(url.contains("?")) {
            return url.substring(0, url.indexOf("?"));
        }
         return url;
    }
}