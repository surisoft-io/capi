package io.surisoft.capi.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.OpaResult;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "oauth2.provider", name = "enabled", havingValue = "true")
public class AuthorizationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationProcessor.class);

    @Autowired
    private HttpUtils httpUtils;

    @Autowired
    private Cache<String, Service> serviceCache;

    @Autowired(required = false)
    private OpaService opaService;

    @Override
    public void process(Exchange exchange) {

        String contextPath = (String) exchange.getIn().getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH);
        String accessToken = httpUtils.processAuthorizationAccessToken(exchange);
        Service service = serviceCache.get(contextToRole(contextPath));
        assert service != null;

        if(accessToken != null) {
            try {
                if(service.getServiceMeta().getOpaRego() != null && opaService != null) {
                    OpaResult opaResult = opaService.callOpa(service.getServiceMeta().getOpaRego(), accessToken);
                    if(!opaResult.isAllowed()) {
                        sendException(exchange, "Not subscribed");
                    }
                } else {
                    JWTClaimsSet jwtClaimsSet = httpUtils.authorizeRequest(accessToken);
                    if(!isApiSubscribed(jwtClaimsSet, contextToRole(contextPath))) {
                        if(!isTokenInGroup(jwtClaimsSet, service.getServiceMeta().getSubscriptionGroup())) {
                            sendException(exchange, "Not subscribed");
                        }
                    }
                }
            } catch (AuthorizationException | BadJOSEException | ParseException | JOSEException | IOException e) {
                log.error(e.getMessage());
                sendException(exchange, e.getMessage());
            }
        } else {
            sendException(exchange, "No authorization header provided");
        }
        propagateAuthorization(exchange, accessToken);
    }

    private void sendException(Exchange exchange, String message) {
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, message);
        exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, HttpStatus.UNAUTHORIZED.value());
        exchange.setException(new AuthorizationException(message));
    }

    private boolean isApiSubscribed(JWTClaimsSet jwtClaimsSet, String role) throws ParseException, JsonProcessingException {
        Map<String, Object> claimSetMap = jwtClaimsSet.getJSONObjectClaim(Oauth2Constants.REALMS_CLAIM);
        if(claimSetMap != null && claimSetMap.containsKey(Oauth2Constants.ROLES_CLAIM)) {
            List<String> roleList = (List<String>) claimSetMap.get(Oauth2Constants.ROLES_CLAIM);
            for(String claimRole : roleList) {
                if(claimRole.equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTokenInGroup(JWTClaimsSet jwtClaimsSet, String groups) throws ParseException, JsonProcessingException {
        if(groups != null) {
            List<String> groupList = Collections.singletonList(groups);
            List<String> subscriptionGroupList =  jwtClaimsSet.getStringListClaim(Oauth2Constants.SUBSCRIPTIONS_CLAIM);
            for(String subscriptionGroup : subscriptionGroupList) {
                for(String apiGroup : groupList) {
                    if(normalizeGroup(apiGroup).equals(normalizeGroup(subscriptionGroup))) {
                        return true;
                    }
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

    private String normalizeGroup(String group) {
        return group.trim().replaceAll("/", "");
    }

    private void propagateAuthorization(Exchange exchange, String accessToken) {
        if(accessToken != null) {
            exchange.getIn().setHeader(Constants.AUTHORIZATION_HEADER, Constants.BEARER + accessToken);
        }
    }
}