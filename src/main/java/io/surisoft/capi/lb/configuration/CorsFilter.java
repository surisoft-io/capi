package io.surisoft.capi.lb.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

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
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}