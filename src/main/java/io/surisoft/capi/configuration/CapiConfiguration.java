package io.surisoft.capi.configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxMeterRegistry;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.tracer.CapiTracer;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import okhttp3.OkHttpClient;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.component.micrometer.CamelJmxConfig;
import org.apache.camel.component.micrometer.DistributionStatisticConfigFilter;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy.MESSAGE_HISTORIES;
import static org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy.ROUTE_POLICIES;

@Configuration
public class CapiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CapiConfiguration.class);

    @Value("${capi.traces.endpoint}")
    private String tracesEndpoint;

    @Autowired
    HttpUtils httpUtils;

    @Bean
    @ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
    public Map<String, WebsocketClient> websocketClients() {
        return new HashMap<>();
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.traces", name = "enabled", havingValue = "true")
    CapiTracer capiTracer(CamelContext camelContext) {
        camelContext.setUseMDCLogging(true);

        log.debug("Traces Enabled!");
        Set<String> excludePatterns = new HashSet<>();
        excludePatterns.add("timer://");
        excludePatterns.add("bean://consulNodeDiscovery");
        CapiTracer capiTracer = new CapiTracer(httpUtils);
        OkHttpSender okHttpSender = OkHttpSender.create(tracesEndpoint);
        capiTracer.setSpanReporter(AsyncReporter.create(okHttpSender));
        capiTracer.setIncludeMessageBody(true);
        capiTracer.setIncludeMessageBodyStreams(true);

        capiTracer.setExcludePatterns(excludePatterns);
        capiTracer.init(camelContext);
        return capiTracer;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.disable", name = "redirect", havingValue = "true")
    public HttpComponent disableFollowRedirect(CamelContext camelContext) {
        HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("http");
        HttpClientConfigurer httpClientConfigurer = HttpClientBuilder::disableRedirectHandling;
        httpComponent.setHttpClientConfigurer(httpClientConfigurer);
        return httpComponent;
    }

    @Bean
    public CompositeMeterRegistry metrics() {

        DistributionStatisticConfigFilter timerMeterFilter = new DistributionStatisticConfigFilter()
                .andAppliesTo(ROUTE_POLICIES)
                .orAppliesTo(MESSAGE_HISTORIES)
                .setPublishPercentileHistogram(true)
                .setMinimumExpectedDuration(Duration.ofMillis(1L))
                .setMaximumExpectedDuration(Duration.ofMillis(150L));

        DistributionStatisticConfigFilter summaryMeterFilter = new DistributionStatisticConfigFilter()
                .setPublishPercentileHistogram(true)
                .setMinimumExpectedValue(1L)
                .setMaximumExpectedValue(100L);


        CompositeMeterRegistry compositeMeterRegistry = new CompositeMeterRegistry();
        compositeMeterRegistry.config().commonTags(Tags.of("application", Constants.APPLICATION_NAME))
                .meterFilter(timerMeterFilter)
                .meterFilter(summaryMeterFilter).namingConvention().tagKey(Constants.APPLICATION_NAME);

        compositeMeterRegistry.add(new JmxMeterRegistry(
                CamelJmxConfig.DEFAULT,
                Clock.SYSTEM,
                HierarchicalNameMapper.DEFAULT));
        return compositeMeterRegistry;
    }

    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient();
    }

    @Bean(name = "capiCorsFilterStrategy")
    @ConditionalOnProperty(prefix = "capi.gateway.cors", name = "management.enabled", havingValue = "true")
    public CapiCorsFilterStrategy capiCorsFilterStrategy() {
        return new CapiCorsFilterStrategy();
    }
}