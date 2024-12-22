package io.surisoft.capi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.CapiRestError;
import io.surisoft.capi.schema.OpaResult;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.service.OpaService;
import io.undertow.server.HttpServerExchange;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpCookie;
import java.text.ParseException;
import java.util.*;

@Component
public class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private final String authorizationCookieName;
    private final Optional<List<DefaultJWTProcessor<SecurityContext>>> jwtProcessorList;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpUtils(@Value("${capi.oauth2.cookieName}") String authorizationCookieName,
                     Optional<List<DefaultJWTProcessor<SecurityContext>>> jwtProcessorList) {
        this.authorizationCookieName = authorizationCookieName;
        this.jwtProcessorList = jwtProcessorList;
    }

    public String setHttpConnectTimeout(String endpoint, int timeout) {
        return prepareEndpoint(endpoint) + Constants.HTTP_CONNECT_TIMEOUT + timeout;
    }

    public String setHttpSocketTimeout(String endpoint, int timeout) {
        return prepareEndpoint(endpoint) + Constants.HTTP_SOCKET_TIMEOUT + timeout;
    }

    public String setIngressEndpoint(String endpoint, String hostName) {
        return prepareEndpoint(endpoint) + Constants.CUSTOM_HOST_HEADER + hostName;
    }

    public String getCapiContext(String context) {
        return context.substring(0, context.indexOf("/*"));
    }

    private String prepareEndpoint(String endpoint) {
        if(endpoint.contains("?")) {
            if (!endpoint.endsWith("&")) {
                endpoint = endpoint + "&";
            }
        } else {
            endpoint = endpoint + "?";
        }
        return endpoint;
    }

    public String getBearerTokenFromHeader(String authorizationHeader) throws AuthorizationException {
        try {
            return authorizationHeader.substring(7);
        } catch(Exception e) {
            throw new AuthorizationException("Invalid authorization provided");
        }
    }

    public JWTClaimsSet authorizeRequest(String accessToken) throws AuthorizationException {
        Exception exception = null;
        if(jwtProcessorList.isPresent()) {
            for(DefaultJWTProcessor<SecurityContext> jwtProcessor : jwtProcessorList.get()) {
                try {
                    return jwtProcessor.process(accessToken, null);
                } catch(BadJOSEException | ParseException | JOSEException e)  {
                    exception = e;
                }
            }
            if(exception != null) {
                throw new AuthorizationException(exception.getMessage());
            }
        }
        return null;
    }

    public String processAuthorizationAccessToken(Exchange exchange) throws AuthorizationException {
        String authorization = exchange.getIn().getHeader(Constants.AUTHORIZATION_HEADER, String.class);
        if(authorization == null) {
            if(exchange.getIn().getHeader(Constants.AUTHORIZATION_REQUEST_PARAMETER, String.class) != null) {
                return exchange.getIn().getHeader(Constants.AUTHORIZATION_REQUEST_PARAMETER, String.class);
            }
            List<HttpCookie> cookies = getCookiesFromExchange(exchange);
            String authorizationName = exchange.getIn().getHeader(authorizationCookieName, String.class);
            if(authorizationName != null) {
                return getAuthorizationCookieValue(cookies, authorizationName);
            }
        } else {
            return getBearerTokenFromHeader(authorization);
        }
        return null;
    }

    public String processAuthorizationAccessToken(HttpServerExchange httpServerExchange) throws AuthorizationException {
        String accessToken = null;
        if(httpServerExchange.getRequestHeaders().contains(Constants.AUTHORIZATION_HEADER)) {
            accessToken = getBearerTokenFromHeader(httpServerExchange.getRequestHeaders().get(Constants.AUTHORIZATION_HEADER).get(0));
        } else if(httpServerExchange.getQueryParameters().containsKey("access_token")) {
            accessToken = httpServerExchange.getQueryParameters().get("access_token").getFirst();
        }
        return accessToken;
    }

    public String processAuthorizationAccessToken(HttpServletRequest httpServletRequest) throws AuthorizationException {
        String authorization = httpServletRequest.getHeader(Constants.AUTHORIZATION_HEADER);
        if(authorization == null) {
            if(httpServletRequest.getHeader(Constants.AUTHORIZATION_REQUEST_PARAMETER) != null) {
                return httpServletRequest.getHeader(Constants.AUTHORIZATION_REQUEST_PARAMETER);
            }
            List<HttpCookie> cookies = getCookiesFromRequest(httpServletRequest);
            String authorizationName = httpServletRequest.getHeader(authorizationCookieName);
            if(authorizationName != null) {
                return getAuthorizationCookieValue(cookies, authorizationName);
            }
        } else {
            return getBearerTokenFromHeader(authorization);
        }
        return null;
    }

    public String normalizeHttpEndpoint(String httpEndpoint) {
        if(httpEndpoint.contains("http://")) {
            return httpEndpoint.replace("http://", "");
        }
        if(httpEndpoint.contains("https://")) {
            return httpEndpoint.replace("https://", "");
        }
        return httpEndpoint;
    }

    public boolean isEndpointSecure(String httpEndpoint) {
        return httpEndpoint.contains("https://");
    }

    public List<HttpCookie> getCookiesFromExchange(Exchange exchange) {
        List<HttpCookie> httpCookieList = new ArrayList<>();
        try {
            if(exchange.getIn().getHeader(Constants.COOKIE_HEADER) != null) {
                String[] cookieArray = exchange.getIn().getHeader(Constants.COOKIE_HEADER, String.class).split(";");
                for (String cookieString : cookieArray) {
                    String[] cookieKeyValue = cookieString.split("=");
                    HttpCookie httpCookie = new HttpCookie(stripOffSurroundingQuote(cookieKeyValue[0]), stripOffSurroundingQuote(cookieKeyValue[1]));
                    httpCookieList.add(httpCookie);
                }
            }
        } catch(Exception e) {
            return httpCookieList;
        }
        return httpCookieList;
    }

    public List<HttpCookie> getCookiesFromRequest(HttpServletRequest httpServletRequest) {
        List<HttpCookie> httpCookieList = new ArrayList<>();
        if(httpServletRequest.getHeader(Constants.COOKIE_HEADER) != null) {
            String[] cookieArray = httpServletRequest.getHeader(Constants.COOKIE_HEADER).split(";");
            for (String cookieString : cookieArray) {
                String[] cookieKeyValue = cookieString.split("=");
                HttpCookie httpCookie = new HttpCookie(stripOffSurroundingQuote(cookieKeyValue[0]), stripOffSurroundingQuote(cookieKeyValue[1]));
                httpCookieList.add(httpCookie);
            }
        }
        return httpCookieList;
    }

    public String getAuthorizationCookieValue(List<HttpCookie> httpCookieList, String authorizationCookie) {
        for(HttpCookie httpCookie : httpCookieList) {
            if(httpCookie.getName().equals(authorizationCookie)) {
                return httpCookie.getValue();
            }
        }
        return null;
    }

    private static String stripOffSurroundingQuote(String value) {

        if (value != null && value.length() > 2 &&
                value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        if (value != null && value.length() > 2 &&
                value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    public boolean isAuthorized(String accessToken, String contextPath, Service service, OpaService opaService) {
        try {
            if(service.getServiceMeta().getOpaRego() != null && opaService != null) {
                OpaResult opaResult = opaService.callOpa(service.getServiceMeta().getOpaRego(), accessToken, true);
                if(!opaResult.isAllowed()) {
                    return false;
                }
            } else {
                JWTClaimsSet jwtClaimsSet = authorizeRequest(accessToken);
                if(!isApiSubscribed(jwtClaimsSet, contextToRole(contextPath))) {
                    if(!isTokenInGroup(jwtClaimsSet, service.getServiceMeta().getSubscriptionGroup())) {
                        //Not subscribed
                        return false;
                    }
                }
            }
        } catch (AuthorizationException | ParseException | IOException e) {
            log.error(e.getMessage());
            //General Exception
            return false;
        }
        return true;
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

    public String contextToRole(String context) {
        if(context.startsWith("/")) {
            context = context.substring(1);
        }
        return context.replace("/", ":");
    }

    private String normalizeGroup(String group) {
        return group.trim().replaceAll("/", "");
    }

    public String proxyErrorMapper(CapiRestError capiRestError) {
        try {
            return objectMapper.writeValueAsString(capiRestError);
        } catch (JsonProcessingException e) {
            return "no-message";
        }
    }

    public void sendException(Exchange exchange, String message) {
        String validatedMessage = validateHeaderValue(message);
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, validatedMessage);
        exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, HttpStatus.UNAUTHORIZED.value());
        exchange.setException(new AuthorizationException(message));
    }

    public static String validateHeaderValue(String value) {
        if (!value.matches("^[a-zA-Z0-9 \\-]+$")) {
            throw new IllegalArgumentException("Invalid header value");
        }
        return value;
    }
}