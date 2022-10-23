package io.surisoft.capi.lb.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.lb.exception.AuthorizationException;
import io.surisoft.capi.lb.oidc.OIDCConstants;
import io.surisoft.capi.lb.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "oidc.provider", name = "enabled", havingValue = "true")
public class AuthorizationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationProcessor.class);
    @Autowired
    private DefaultJWTProcessor<SecurityContext> jwtProcessor;

    @Override
    public void process(Exchange exchange) {
        String contextPath = (String) exchange.getIn().getHeader(OIDCConstants.CAMEL_SERVLET_CONTEXT_PATH);
        String authorizationHeader = exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class);

        if(authorizationHeader != null) {
            try {
                JWTClaimsSet jwtClaimsSet = authorizeRequest(authorizationHeader);
                if(!isApiSubscribed(jwtClaimsSet, contextToRole(contextPath))) {
                    sendException(exchange, "Not subscribed");
                }
            } catch (AuthorizationException | BadJOSEException | ParseException | JOSEException | IOException e) {
                log.error(e.getMessage());
                sendException(exchange, e.getMessage());
            }
        } else {
            sendException(exchange, "No authorization header provided");
        }
    }

    private JWTClaimsSet authorizeRequest(String authorizationHeader) throws AuthorizationException, BadJOSEException, ParseException, JOSEException, IOException {
        return jwtProcessor.process(getBearerTokenFromHeader(authorizationHeader), null);
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

    private String getBearerTokenFromHeader(String authorizationHeader) {
        return authorizationHeader.substring(7);
    }

    private String contextToRole(String context) {
        if(context.startsWith("/")) {
            context = context.substring(1);
        }
        return context.replace("/", ":");
    }
}