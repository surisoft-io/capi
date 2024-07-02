package io.surisoft.capi.configuration;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.utils.Constants;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CapiCorsFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CapiCorsFilter.class);
    private final String oauth2CookieName;
    private final boolean gatewayCorsManagementEnabled;
    private String capiContextPath;
    private final Cache<String, Service> serviceCache;
    private final List<String> accessControlAllowHeaders;
    private final Map<String, String> managedHeaders;

    public CapiCorsFilter(@Value("${capi.oauth2.cookieName}") String oauth2CookieName,
                          @Value("${capi.gateway.cors.management.enabled}") boolean gatewayCorsManagementEnabled,
                          @Value("${camel.servlet.mapping.context-path}") String capiContextPath,
                          @Value("${capi.gateway.cors.management.allowed-headers}") List<String> accessControlAllowHeaders,
                          Cache<String, Service> serviceCache) {
        this.oauth2CookieName = oauth2CookieName;
        this.gatewayCorsManagementEnabled = gatewayCorsManagementEnabled;
        this.capiContextPath = capiContextPath;
        this.accessControlAllowHeaders = accessControlAllowHeaders;
        this.serviceCache = serviceCache;

        managedHeaders = new java.util.HashMap<>(Constants.CAPI_CORS_MANAGED_HEADERS);
        managedHeaders.put("Access-Control-Allow-Headers", StringUtils.join(accessControlAllowHeaders, ","));
    }

    @PostConstruct
    public void corsFilterComponent() {
        if(gatewayCorsManagementEnabled) {
            capiContextPath = capiContextPath.replaceAll("\\*", "");
            log.info("CAPI Gateway CORS Management Enabled");
        }
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        List<String> localAccessControlAllowHeaders = new ArrayList<>(accessControlAllowHeaders);

        if(request.getRequestURI().startsWith(capiContextPath) && gatewayCorsManagementEnabled) {
            if(oauth2CookieName != null && !oauth2CookieName.isEmpty()) {
                localAccessControlAllowHeaders.add(oauth2CookieName);
            }
            processControlledHeaders(localAccessControlAllowHeaders, response, request, request.getHeader(Constants.ORIGIN_HEADER), true);
        }

        if (request.getMethod().equals(Constants.OPTIONS_METHODS_VALUE)) {
            response.setHeader("Access-Control-Max-Age", Constants.ACCESS_CONTROL_MAX_AGE_VALUE);
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void processControlledHeaders(List<String> localAccessControlAllowHeaders, HttpServletResponse response, HttpServletRequest request, String origin, boolean capiConsumer) {
        managedHeaders.forEach((k, v) -> {
            if(k.equals(Constants.ACCESS_CONTROL_ALLOW_HEADERS)) {
                v = StringUtils.join(localAccessControlAllowHeaders, ",");
            }
            response.setHeader(k, v);
        });
        processOrigin(response, request, origin, capiConsumer);
    }

    private void processOrigin(HttpServletResponse response, HttpServletRequest request, String origin, boolean capiConsumer) {
        if(isValidOrigin(origin)) {
            if(capiConsumer) {
                if(isOriginAllowed(request)) {
                    response.setHeader(Constants.ACCESS_CONTROL_ALLOW_ORIGIN, origin.replaceAll("(\r\n|\n)", ""));
                }
            } else {
                response.setHeader(Constants.ACCESS_CONTROL_ALLOW_ORIGIN, origin.replaceAll("(\r\n|\n)", ""));
            }
        }
    }

    private boolean isOriginAllowed(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String serviceId = requestURI.trim().split("/")[2] + ":" + requestURI.split("/")[3];
        Service service = serviceCache.peek(serviceId);
        if(service != null && service.getServiceMeta() != null) {
            if(service.getServiceMeta().getAllowedOrigins() != null) {
                List<String> allowedOriginsList = Arrays.asList(service.getServiceMeta().getAllowedOrigins().split(",", -1));
                return allowedOriginsList.contains(request.getHeader(Constants.ORIGIN_HEADER));
            } else {
                return true;
            }
        }
        return true;
    }

    private boolean isValidOrigin(String origin) {
        try {
            new URL(origin).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}