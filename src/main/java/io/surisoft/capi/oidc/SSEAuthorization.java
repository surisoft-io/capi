package io.surisoft.capi.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.schema.SSEClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SSEAuthorization {
    private static final Logger log = LoggerFactory.getLogger(SSEAuthorization.class);
    private final List<DefaultJWTProcessor<SecurityContext>> jwtProcessorList;

    public SSEAuthorization(List<DefaultJWTProcessor<SecurityContext>> jwtProcessorList) {
        this.jwtProcessorList = jwtProcessorList;
    }

    public boolean isAuthorized(SSEClient sseClient, Request request) {
        if (!sseClient.requiresSubscription()) {
            return true;
        }
        Fields queryParams = Request.extractQueryParameters(request);
        if (request.getHeaders().contains(Oauth2Constants.AUTHORIZATION_HEADER)
                || queryParams.get(Oauth2Constants.AUTHORIZATION_QUERY) != null) {
            return isApiSubscribed(request, sseClient.getSubscriptionRole());
        }
        return false;
    }

    public String getBearerTokenFromHeader(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }

    private boolean isApiSubscribed(Request request, String role) {
        String bearerToken;
        if (request.getHeaders().contains(Oauth2Constants.AUTHORIZATION_HEADER)) {
            bearerToken = getBearerTokenFromHeader(request.getHeaders().get(Oauth2Constants.AUTHORIZATION_HEADER));
        } else {
            Fields queryParams = Request.extractQueryParameters(request);
            bearerToken = queryParams.get(Oauth2Constants.AUTHORIZATION_QUERY).getValue();
            removeAuthorizationFromQuery(request);
        }
        try {
            JWTClaimsSet jwtClaimsSet = tryToValidateToken(bearerToken);
            Map<String, Object> claimSetMap = Objects.requireNonNull(jwtClaimsSet).getJSONObjectClaim(Oauth2Constants.REALMS_CLAIM);
            if (claimSetMap != null && claimSetMap.containsKey(Oauth2Constants.ROLES_CLAIM)) {
                List<String> roleList = (List<String>) claimSetMap.get(Oauth2Constants.ROLES_CLAIM);
                for (String claimRole : roleList) {
                    if (claimRole.equals(role)) {
                        return true;
                    }
                }
            }
            if (isTokenInGroup(jwtClaimsSet, role)) {
                return true;
            }
        } catch (ParseException | NullPointerException e) {
            log.warn(e.getMessage(), e);
        }
        return false;
    }

    private void removeAuthorizationFromQuery(Request request) {
        Fields queryParams = Request.extractQueryParameters(request);
        StringBuilder queryString = new StringBuilder();
        for (Fields.Field field : queryParams) {
            if (!field.getName().equals(Oauth2Constants.AUTHORIZATION_QUERY)) {
                if (!queryString.isEmpty()) {
                    queryString.append("&");
                }
                queryString.append(field.getName()).append("=").append(field.getValue());
            }
        }
        request.setAttribute(io.surisoft.capi.utils.Constants.SANITIZED_QUERY_ATTR, queryString.toString());
    }

    private JWTClaimsSet tryToValidateToken(String bearerToken) {
        for (DefaultJWTProcessor<SecurityContext> jwtProcessor : jwtProcessorList) {
            try {
                return jwtProcessor.process(bearerToken, null);
            } catch (ParseException | BadJOSEException | JOSEException ignored) {}
        }
        return null;
    }

    private boolean isTokenInGroup(JWTClaimsSet jwtClaimsSet, String groups) {
        if (groups != null) {
            try {
                List<String> groupList = Collections.singletonList(groups);
                List<String> subscriptionGroupList = jwtClaimsSet.getStringListClaim(Oauth2Constants.SUBSCRIPTIONS_CLAIM);
                for (String subscriptionGroup : subscriptionGroupList) {
                    for (String apiGroup : groupList) {
                        if (normalizeGroup(apiGroup).equals(normalizeGroup(subscriptionGroup))) {
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
