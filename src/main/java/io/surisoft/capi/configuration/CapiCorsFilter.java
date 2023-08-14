package io.surisoft.capi.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CapiCorsFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CapiCorsFilter.class);

    @Value("${capi.manager.cors.host}")
    private String capiManagerCorsHost;

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if((request.getRequestURI().startsWith("/manager"))) {
            response.setHeader("Access-Control-Allow-Credentials", "false");
            response.setHeader("Access-Control-Allow-Origin", capiManagerCorsHost);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
            response.setHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization");
            response.setHeader("Access-Control-Max-Age", "1728000");
        } /*else if((request.getRequestURI().startsWith("/capi"))) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
            response.setHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization");
            response.setHeader("Access-Control-Max-Age", "1728000");
        }*/
        filterChain.doFilter(servletRequest, servletResponse);
    }
}