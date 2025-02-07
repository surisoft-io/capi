package io.surisoft.capi.configuration;

import io.surisoft.capi.tracer.CapiTracer;
import io.surisoft.capi.tracer.CapiUndertowTracer;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

@Configuration
@ConditionalOnProperty(prefix = "capi.traces", name = "enabled", havingValue = "true")
public class CapiTracerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CapiTracerConfiguration.class);
    private final String tracesEndpoint;
    private final HttpUtils httpUtils;

    public CapiTracerConfiguration(@Value("${capi.traces.endpoint}") String tracesEndpoint,
                                   HttpUtils httpUtils) {
        this.tracesEndpoint = tracesEndpoint;
        this.httpUtils = httpUtils;
    }

    @Bean
    public CapiTracer capiTracer(CamelContext camelContext) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        camelContext.setUseMDCLogging(true);

        log.debug("Traces Enabled!");

        Set<String> excludePatterns = new HashSet<>();
        excludePatterns.add("timer://");
        excludePatterns.add("bean://consulNodeDiscovery");
        excludePatterns.add("bean://consistencyChecker");

        CapiTracer capiTracer = new CapiTracer(httpUtils);

        URLConnectionSender sender = URLConnectionSender
                .newBuilder()
                .readTimeout(100)
                .endpoint(tracesEndpoint + "/api/v2/spans")
                .build();

        capiTracer.setSpanReporter(AsyncReporter.builder(sender).build());
        capiTracer.setIncludeMessageBody(true);
        capiTracer.setIncludeMessageBodyStreams(true);

        capiTracer.init(camelContext);
        return capiTracer;
    }

    @Bean
    public CapiUndertowTracer capiUndertowTracer() throws Exception {
        log.debug("Undertow Traces Enabled!");

        CapiUndertowTracer capiUndertowTracer = new CapiUndertowTracer(httpUtils);

        URLConnectionSender sender = URLConnectionSender
                .newBuilder()
                .readTimeout(100)
                .endpoint(tracesEndpoint + "/api/v2/spans")
                .build();

        capiUndertowTracer.setSpanReporter(AsyncReporter.builder(sender).build());
        capiUndertowTracer.init();
        return capiUndertowTracer;
    }
}