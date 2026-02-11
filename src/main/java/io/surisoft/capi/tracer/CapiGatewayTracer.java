package io.surisoft.capi.tracer;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.ErrorMessage;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class CapiGatewayTracer {
    private static final Logger log = LoggerFactory.getLogger(CapiGatewayTracer.class);
    private final CapiTracingState tracingState = new CapiTracingState();
    private final Tracer tracer;
    private final HttpUtils httpUtils;

    public CapiGatewayTracer(HttpUtils httpUtils, Tracer tracer) {
        this.httpUtils = httpUtils;
        this.tracer = tracer;
    }

    public void init() throws Exception {
        doInit();
    }

    protected void doInit() throws Exception {
        ObjectHelper.notNull(tracer, "Tracer", this);
    }

    public void serverRequest(Request request, String serviceName) {
        Span span = tracer.spanBuilder(serviceName != null ? serviceName : "unknown")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        if (request.getHeaders().contains("Connection")) {
            span.setAttribute("Connection", request.getHeaders().get("Connection"));
        }

        if (request.getHeaders().contains("Sec-WebSocket-Version")) {
            span.setAttribute("Sec-WebSocket-Version", request.getHeaders().get("Sec-WebSocket-Version"));
        }

        if (request.getHeaders().contains("Sec-WebSocket-Key")) {
            span.setAttribute("Sec-WebSocket-Key", request.getHeaders().get("Sec-WebSocket-Key"));
        }

        org.eclipse.jetty.util.Fields queryParams = Request.extractQueryParameters(request);
        if (queryParams.get("clientId") != null) {
            span.updateName(queryParams.get("clientId").getValue());
        } else {
            span.updateName("");
        }

        try {
            String accessToken = httpUtils.processAuthorizationAccessToken(request);
            if (accessToken != null) {
                SignedJWT signedJWT = SignedJWT.parse(accessToken);
                JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                Date expirationTime = jwtClaimsSet.getExpirationTime();
                if (expirationTime.before(Calendar.getInstance().getTime())) {
                    span.setAttribute(Constants.CAPI_TOKEN_EXPIRED, Boolean.toString(true));
                } else {
                    span.setAttribute(Constants.CAPI_TOKEN_EXPIRED, Boolean.toString(false));
                }
                String authorizedParty = jwtClaimsSet.getStringClaim(Constants.AUTHORIZED_PARTY);
                if (authorizedParty != null) {
                    span.setAttribute(Constants.CAPI_EXCHANGE_REQUESTER_ID, authorizedParty);
                }
                span.setAttribute(Constants.CAPI_REQUESTER_TOKEN_ISSUER, jwtClaimsSet.getIssuer());
            } else {
                span.setAttribute(Constants.CAPI_EXCHANGE_REQUESTER_ID, ErrorMessage.NO_TOKEN_PROVIDED);
            }
        } catch (AuthorizationException | ParseException e) {
            throw new RuntimeException(e);
        }
        tracingState.pushServerSpan(span);
    }

    public void capiProxyRequest(URI host) {
        Span span = tracingState.popServerSpan();
        if (span != null) {
            span.setAttribute(Constants.CAPI_WS_CLIENT_HOST, host.getHost());
            span.setAttribute(Constants.CAPI_WS_CLIENT_PORT, String.valueOf(host.getPort()));
            if (host.getPath() != null && !host.getPath().isEmpty()) {
                span.setAttribute(Constants.CAPI_WS_CLIENT_PATH, host.getPath());
            }
            if (host.getQuery() != null) {
                span.setAttribute(Constants.CAPI_WS_CLIENT_QUERY, host.getQuery());
            }
            span.setAttribute(Constants.CAPI_WS_CLIENT_SCHEME, host.getScheme());
            span.end();
        }
    }
}
