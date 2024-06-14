package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "capi.oauth2.provider", name = "enabled", havingValue = "true")
public class AuthorizationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationProcessor.class);
    private final HttpUtils httpUtils;
    private final Cache<String, Service> serviceCache;
    private final Optional<OpaService> opaService;

    public AuthorizationProcessor(HttpUtils httpUtils, Cache<String, Service> serviceCache, Optional<OpaService> opaService) {
        this.httpUtils = httpUtils;
        this.serviceCache = serviceCache;
        this.opaService = opaService;
    }

    @Override
    public void process(Exchange exchange) {

        String contextPath = (String) exchange.getIn().getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH);
        String accessToken;
        try {
            accessToken = httpUtils.processAuthorizationAccessToken(exchange);
            Service service = serviceCache.get(httpUtils.contextToRole(contextPath));
            assert service != null;

            if(accessToken != null) {
                if(!httpUtils.isAuthorized(accessToken, contextPath, service, (opaService.orElse(null)))) {
                    sendException(exchange, "Not subscribed");
                }
                propagateAuthorization(exchange, accessToken);
            } else {
                sendException(exchange, "No authorization header provided");
            }
        } catch (AuthorizationException e) {
            sendException(exchange, e.getMessage());
        }
    }

    private void sendException(Exchange exchange, String message) {
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, message);
        exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, HttpStatus.UNAUTHORIZED.value());
        exchange.setException(new AuthorizationException(message));
    }

    private void propagateAuthorization(Exchange exchange, String accessToken) {
        if(accessToken != null) {
            exchange.getIn().setHeader(Constants.AUTHORIZATION_HEADER, Constants.BEARER + accessToken.replaceAll("(\r\n|\n)", ""));
        }
    }
}