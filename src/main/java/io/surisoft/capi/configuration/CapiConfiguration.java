package io.surisoft.capi.configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxMeterRegistry;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.zipkin.CapiZipkinTracer;
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

import static org.apache.camel.component.micrometer.MicrometerConstants.DISTRIBUTION_SUMMARIES;
import static org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy.MESSAGE_HISTORIES;
import static org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy.ROUTE_POLICIES;

@Configuration
public class CapiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CapiConfiguration.class);

    @Value("${capi.zipkin.endpoint}")
    private String zipkinEndpoint;

    @Autowired
    HttpUtils httpUtils;

    @Bean
    @ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
    public Map<String, WebsocketClient> websocketClients() {
        Map<String, WebsocketClient> websocketClientMap = new HashMap<>();
        /*WebsocketClient echoClient = new WebsocketClient();
        echoClient.setPort(8888);
        echoClient.setHost("http://localhost");
        echoClient.setPath(Constants.CAPI_CONTEXT + "/echo");
        websocketClientMap.put(Constants.CAPI_CONTEXT + "/echo", echoClient);*/
        return websocketClientMap;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.zipkin", name = "enabled", havingValue = "true")
    CapiZipkinTracer zipkinTracer(CamelContext camelContext) {
        camelContext.setUseMDCLogging(true);

        log.debug("Zipkin Enabled!");
        Set<String> excludePatterns = new HashSet<>();
        excludePatterns.add("timer://");
        excludePatterns.add("bean://consulNodeDiscovery");
        CapiZipkinTracer zipkin = new CapiZipkinTracer(httpUtils);
        OkHttpSender okHttpSender = OkHttpSender.create(zipkinEndpoint);
        zipkin.setSpanReporter(AsyncReporter.create(okHttpSender));
        zipkin.setIncludeMessageBody(true);
        zipkin.setIncludeMessageBodyStreams(true);

        zipkin.setExcludePatterns(excludePatterns);
        zipkin.init(camelContext);
        return zipkin;
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
                .andAppliesTo(DISTRIBUTION_SUMMARIES)
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

}