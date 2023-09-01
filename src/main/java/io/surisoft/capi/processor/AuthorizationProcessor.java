package io.surisoft.capi.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.oidc.OIDCConstants;
import io.surisoft.capi.schema.Api;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import jakarta.servlet.http.Cookie;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.base.cookie.CookieHandler;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "oidc.provider", name = "enabled", havingValue = "true")
public class AuthorizationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationProcessor.class);

    @Autowired
    private HttpUtils httpUtils;

    @Autowired
    private Cache<String, Api> apiCache;


    @Override
    public void process(Exchange exchange) {

        String contextPath = (String) exchange.getIn().getHeader(OIDCConstants.CAMEL_SERVLET_CONTEXT_PATH);
        String accessToken = processAuthorizationAccessToken(exchange);

        log.info(contextToRole(contextPath));
        Api api = apiCache.get(contextToRole(contextPath));

        if(accessToken != null) {
            try {
                JWTClaimsSet jwtClaimsSet = httpUtils.authorizeRequest(accessToken);
                if(!isApiSubscribed(jwtClaimsSet, contextToRole(contextPath))) {
                    assert api != null;
                    if(!isTokenInGroup(jwtClaimsSet, api.getSubscriptionGroup())) {
                        sendException(exchange, "Not subscribed");
                    }
                }
            } catch (AuthorizationException | BadJOSEException | ParseException | JOSEException | IOException e) {
                log.error(e.getMessage());
                sendException(exchange, e.getMessage());
            }
        } else {
            sendException(exchange, "No authorization header provided");
        }
    }

    private void sendException(Exchange exchange, String message) {
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, message);
        exchange.setException(new AuthorizationException(message));
    }

    private boolean isApiSubscribed(JWTClaimsSet jwtClaimsSet, String role) throws ParseException, JsonProcessingException {
        Map<String, Object> claimSetMap = jwtClaimsSet.getJSONObjectClaim(OIDCConstants.REALMS_CLAIM);
        if(claimSetMap != null && claimSetMap.containsKey(OIDCConstants.ROLES_CLAIM)) {
            List<String> roleList = (List<String>) claimSetMap.get(OIDCConstants.ROLES_CLAIM);
            for(String claimRole : roleList) {
                if(claimRole.equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTokenInGroup(JWTClaimsSet jwtClaimsSet, List<String> groupList) throws ParseException, JsonProcessingException {
        List<String> subscriptionGroupList =  jwtClaimsSet.getStringListClaim(OIDCConstants.SUBSCRIPTIONS_CLAIM);
        for(String subscriptionGroup : subscriptionGroupList) {
            for(String apiGroup : groupList) {
                if(normalizeGroup(apiGroup).equals(normalizeGroup(subscriptionGroup))) {
                    return true;
                }
            }
        }
       return false;
    }

    private String contextToRole(String context) {
        if(context.startsWith("/")) {
            context = context.substring(1);
        }
        return context.replace("/", ":");
    }

    private String processAuthorizationAccessToken(Exchange exchange) {
        String authorization = exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class);
        if(authorization == null) {
            List<HttpCookie> cookies = httpUtils.getCookiesFromExchange(exchange);
            String authorizationCookieName = httpUtils.getAuthorizationCookieName(cookies);
            if(authorizationCookieName != null) {
                return httpUtils.getAuthorizationCookieValue(cookies, authorizationCookieName);
            }
        } else {
            return httpUtils.getBearerTokenFromHeader(authorization);
        }
        return null;
    }

    private String normalizeGroup(String group) {
        return group.trim().replaceAll("/", "");
    }
}