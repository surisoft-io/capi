package io.surisoft.capi.configuration;

import io.surisoft.capi.utils.Constants;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CapiCorsFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CapiCorsFilter.class);

    @Value("${capi.manager.cors.host}")
    private String capiManagerCorsHost;

    @Value("${oauth2.cookieName}")
    private String oauth2CookieName;

    @Value("${capi.gateway.cors.management.enabled}")
    private boolean gatewayCorsManagementEnabled;


    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        List<String> accessControlAllowHeaders = new ArrayList<>(List.of(Constants.CAPI_ACCESS_CONTROL_ALLOW_HEADERS));
        if(oauth2CookieName != null && !oauth2CookieName.isEmpty()) {
            accessControlAllowHeaders.add(oauth2CookieName);
        }

        if((request.getRequestURI().startsWith("/manager"))) {
            response.setHeader("Access-Control-Allow-Credentials", "false");
            response.setHeader("Access-Control-Allow-Origin", capiManagerCorsHost);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
            response.setHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization");
            response.setHeader("Access-Control-Max-Age", "1728000");
        } else if(request.getRequestURI().startsWith("/capi/") && gatewayCorsManagementEnabled) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH   ");
            response.setHeader("Access-Control-Max-Age", "1728000");
            processAccessControlAllowHeaders(response, accessControlAllowHeaders);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void processAccessControlAllowHeaders(HttpServletResponse response, List<String> accessControlAllowHeaders) {
        if(response.getHeader("Access-Control-Allow-Headers") == null) {
            response.setHeader("Access-Control-Allow-Headers", StringUtils.join(accessControlAllowHeaders, ","));
        }
    }
}