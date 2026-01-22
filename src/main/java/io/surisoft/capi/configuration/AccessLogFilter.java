package io.surisoft.capi.configuration;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AccessLogFilter implements Filter {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("capi.access");

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            String originalIp = req.getRemoteAddr();
            if(req.getHeader("X-Forwarded-For") != null) {
                originalIp = req.getHeader("X-Forwarded-For");
            }
            ACCESS_LOG.info(
                    "access",
                    net.logstash.logback.argument.StructuredArguments.keyValue("method", req.getMethod()),
                    net.logstash.logback.argument.StructuredArguments.keyValue("path", req.getRequestURI()),
                    net.logstash.logback.argument.StructuredArguments.keyValue("status", res.getStatus()),
                    net.logstash.logback.argument.StructuredArguments.keyValue("duration_ms", durationMs),
                    net.logstash.logback.argument.StructuredArguments.keyValue("remote_ip", originalIp),
                    net.logstash.logback.argument.StructuredArguments.keyValue("referer", req.getHeader("X-Fr")),
                    net.logstash.logback.argument.StructuredArguments.keyValue("user_agent", req.getHeader("User-Agent"))
            );
        }
    }
}