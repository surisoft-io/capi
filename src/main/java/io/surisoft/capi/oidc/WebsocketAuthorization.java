package io.surisoft.capi.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.schema.WebsocketClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WebsocketAuthorization {
    private static final Logger log = LoggerFactory.getLogger(WebsocketAuthorization.class);
    private final List<DefaultJWTProcessor<SecurityContext>> jwtProcessorList;

    public WebsocketAuthorization(List<DefaultJWTProcessor<SecurityContext>> jwtProcessorList) {
        this.jwtProcessorList = jwtProcessorList;
    }

    public boolean isAuthorized(WebsocketClient websocketClient, HttpServerExchange httpServerExchange) {
        if(!websocketClient.requiresSubscription()) {
            return true;
        }
        if(httpServerExchange.getRequestHeaders().contains(Oauth2Constants.AUTHORIZATION_HEADER)
                || httpServerExchange.getQueryParameters().containsKey(Oauth2Constants.AUTHORIZATION_QUERY)) {
            return isApiSubscribed(httpServerExchange, websocketClient.getSubscriptionRole());
        }
        return false;
    }

    public String getBearerTokenFromHeader(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }

    private boolean isApiSubscribed(HttpServerExchange httpServerExchange, String role) {
        String bearerToken;
        if(httpServerExchange.getRequestHeaders().contains(Oauth2Constants.AUTHORIZATION_HEADER)) {
            bearerToken = getBearerTokenFromHeader(httpServerExchange.getRequestHeaders().get(Oauth2Constants.AUTHORIZATION_HEADER, 0));
            //----------------
            //httpServerExchange.getRequestHeaders().remove(new HttpString(Oauth2Constants.AUTHORIZATION_HEADER));
        } else {
            bearerToken = httpServerExchange.getQueryParameters().get(Oauth2Constants.AUTHORIZATION_QUERY).getFirst();
            removeAuthorizationFromQuery(httpServerExchange);
        }
        try {
            JWTClaimsSet jwtClaimsSet = tryToValidateToken(bearerToken);
            if(jwtClaimsSet != null) {
                Map<String, Object> claimSetMap = jwtClaimsSet.getJSONObjectClaim(Oauth2Constants.REALMS_CLAIM);
                if(claimSetMap != null && claimSetMap.containsKey(Oauth2Constants.ROLES_CLAIM)) {
                    List<String> roleList = (List<String>) claimSetMap.get(Oauth2Constants.ROLES_CLAIM);
                    for(String claimRole : roleList) {
                        if(claimRole.equals(role)) {
                            return true;
                        }
                    }
                }
            }

            if(isTokenInGroup(jwtClaimsSet, "capi")) {
                return true;
            }
        } catch (ParseException e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }

    private void removeAuthorizationFromQuery(HttpServerExchange httpServerExchange) {
        StringBuilder queryString = new StringBuilder();
        httpServerExchange.getQueryParameters().forEach((key, value) -> {
            if(!key.equals(Oauth2Constants.AUTHORIZATION_QUERY)) {
                if(queryString.isEmpty()) {
                    queryString
                            .append(key)
                            .append("=")
                            .append(value.getFirst());
                } else {
                    queryString
                            .append("&")
                            .append(key)
                            .append("=")
                            .append(value.getFirst());
                }
            }
        });
        httpServerExchange.getQueryParameters().clear();
        httpServerExchange.setQueryString(queryString.toString());
    }

    private JWTClaimsSet tryToValidateToken(String bearerToken) {
        for(DefaultJWTProcessor<SecurityContext> jwtProcessor : jwtProcessorList) {
            try {
                return jwtProcessor.process(bearerToken, null);
            } catch (ParseException | BadJOSEException | JOSEException ignored) {}
        }
        return null;
    }

    private boolean isTokenInGroup(JWTClaimsSet jwtClaimsSet, String groups) {
        if(groups != null) {
            try {
                List<String> groupList = Collections.singletonList(groups);
                List<String> subscriptionGroupList = null;
                subscriptionGroupList = jwtClaimsSet.getStringListClaim(Oauth2Constants.SUBSCRIPTIONS_CLAIM);
                for(String subscriptionGroup : subscriptionGroupList) {
                    for(String apiGroup : groupList) {
                        if(normalizeGroup(apiGroup).equals(normalizeGroup(subscriptionGroup))) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String normalizeGroup(String group) {
        return group.trim().replaceAll("/", "");
    }
}