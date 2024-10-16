package io.surisoft.capi.tracer;

import brave.Span;
import brave.Tracing;
import brave.sampler.Sampler;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.ErrorMessage;
import io.surisoft.capi.utils.HttpUtils;
import io.undertow.server.HttpServerExchange;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.zipkin.ZipkinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.brave.ZipkinSpanHandler;

import java.io.Closeable;
import java.net.URI;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CapiUndertowTracer {
    private static final Logger log = LoggerFactory.getLogger(CapiUndertowTracer.class);
    ZipkinState undertowZipkinState = new ZipkinState();
    private final Map<String, Tracing> tracingMap = new HashMap<>();
    private Reporter<zipkin2.Span> spanReporter;
    private final HttpUtils httpUtils;

    public CapiUndertowTracer(HttpUtils httpUtils) {
        this.httpUtils = httpUtils;
    }

    public void init() throws Exception {
        doInit();
    }

    public void setSpanReporter(Reporter<zipkin2.Span> spanReporter) {
        this.spanReporter = spanReporter;
    }

    protected void doInit() throws Exception {
        ObjectHelper.notNull(spanReporter, "Reporter<zipkin2.Span>", this);
    }

    protected void doShutdown() {
        ServiceHelper.stopAndShutdownService(spanReporter);
        if (spanReporter instanceof Closeable) {
            IOHelper.close((Closeable) spanReporter);
        }
        tracingMap.clear();
    }

    private Tracing newTracing(String serviceName) {
        float rate = 1.0f;
        return Tracing
                .newBuilder()
                .localServiceName(serviceName)
                .sampler(Sampler.create(rate))
                .addSpanHandler(ZipkinSpanHandler.create(spanReporter))
                .build();
    }

    private Tracing getTracing(String serviceName) {
        Tracing brave = null;
        if (serviceName != null) {
            brave = tracingMap.get(serviceName);
            if (brave == null) {
                log.debug("Creating Tracing assigned to serviceName: {}", serviceName);
                brave = newTracing(serviceName);
                tracingMap.put(serviceName, brave);
            }
        }
        return brave;
    }

    public void serverRequest(HttpServerExchange httpServerExchange, String serviceName) {
        Span span;
        Tracing brave = getTracing(serviceName);

        span = brave.tracer().nextSpan();
        span.kind(Span.Kind.SERVER).start();
        span.customizer();

        if(httpServerExchange.getRequestHeaders().contains("Connection")) {
            span.tag("Connection", httpServerExchange.getRequestHeaders().getFirst("Connection"));
        }

        if(httpServerExchange.getRequestHeaders().contains("Sec-WebSocket-Version")) {
            span.tag("Sec-WebSocket-Version", httpServerExchange.getRequestHeaders().getFirst("Sec-WebSocket-Version"));
        }

        if(httpServerExchange.getRequestHeaders().contains("Sec-WebSocket-Key")) {
            span.tag("Sec-WebSocket-Key", httpServerExchange.getRequestHeaders().getFirst("Sec-WebSocket-Key"));
        }

        if(httpServerExchange.getQueryParameters().containsKey("clientId")) {
            span.name(httpServerExchange.getQueryParameters().get("clientId").getFirst());
        } else {
            span.name("");
        }

        try {
            String accessToken = httpUtils.processAuthorizationAccessToken(httpServerExchange);
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
                span.tag(Constants.CAPI_REQUESTER_TOKEN_ISSUER, jwtClaimsSet.getIssuer());
            } else {
                span.tag(Constants.CAPI_EXCHANGE_REQUESTER_ID, ErrorMessage.NO_TOKEN_PROVIDED);
            }
        } catch (AuthorizationException | ParseException e) {
            throw new RuntimeException(e);
        }
        undertowZipkinState.pushServerSpan(span);
    }

    public void capiProxyRequest(URI host) {
        Span span;
        span = undertowZipkinState.popServerSpan();
        if(span != null) {
            span.tag(Constants.CAPI_WS_CLIENT_HOST, host.getHost());
            span.tag(Constants.CAPI_WS_CLIENT_PORT, String.valueOf(host.getPort()));
            if(host.getPath() != null && !host.getPath().isEmpty()) {
                span.tag(Constants.CAPI_WS_CLIENT_PATH, host.getPath());
            }
            if(host.getQuery() != null) {
                span.tag(Constants.CAPI_WS_CLIENT_QUERY, host.getQuery());
            }
            span.tag(Constants.CAPI_WS_CLIENT_SCHEME, host.getScheme());
            span.finish();
        }
    }
}