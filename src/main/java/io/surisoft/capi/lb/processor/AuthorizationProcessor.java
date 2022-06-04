package io.surisoft.capi.lb.processor;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.lb.exception.AuthorizationException;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.utils.Constants;
import io.surisoft.capi.lb.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationProcessor.class);

    private final Cache<String, Api> apiCache;
    private final HttpUtils httpUtils;

    public AuthorizationProcessor(Cache<String, Api> apiCache, HttpUtils httpUtils) {
        this.apiCache = apiCache;
        this.httpUtils = httpUtils;
    }

    @Override
    public void process(Exchange exchange) {
        String apiId = exchange.getIn().getHeader(Constants.API_ID_HEADER, String.class);
        String authorizationHeader = exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class);

        if(authorizationHeader == null) {
            sendException(exchange, "No authorization header provided");
        }
        if(apiId == null) {
            sendException(exchange, "CAPI was not able to identify your request, please redeploy your Api.");
        }
        Api cachedApi = apiCache.peek(apiId);
        if(cachedApi == null) {
            sendException(exchange, "Your API was not detected, please redeploy.");
        }
        if(cachedApi.getAuthorizationEndpointPublicKey() == null) {
            sendException(exchange, "CAPI was not able to identity your Authorization provider, please redeploy.");
        }
        log.trace("Processing authorization for API: {}", apiId);

        try {
            authorizeRequest(cachedApi, authorizationHeader);
        } catch(AuthorizationException e) {
            sendException(exchange, e.getMessage());
        }

    }

    private void authorizeRequest(Api cachedApi, String authorizationHeader) throws AuthorizationException {
        try {
            DefaultJWTProcessor jwtProcessor = new DefaultJWTProcessor();
            JWKSet jwkSet = JWKSet.parse(cachedApi.getAuthorizationEndpointPublicKey());
            ImmutableJWKSet keySource = new ImmutableJWKSet(jwkSet);
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);
            jwtProcessor.process(httpUtils.getBearerTokenFromHeader(authorizationHeader), null);
        } catch(Exception e) {
            throw new AuthorizationException(e.getMessage());
        }
    }

    private void sendException(Exchange exchange, String message) {
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, message);
        exchange.setException(new AuthorizationException(message));
    }
}