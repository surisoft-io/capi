package io.surisoft.capi.configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxMeterRegistry;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.CapiTrustManager;
import io.surisoft.capi.service.ConsistencyChecker;
import io.surisoft.capi.service.ConsulKVStore;
import io.surisoft.capi.tracer.CapiTracer;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import io.surisoft.capi.utils.RouteUtils;
import okhttp3.OkHttpClient;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.component.micrometer.CamelJmxConfig;
import org.apache.camel.component.micrometer.DistributionStatisticConfigFilter;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.zipkin2.RestTemplateSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import zipkin2.reporter.AsyncReporter;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.*;

import static org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy.MESSAGE_HISTORIES;
import static org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy.ROUTE_POLICIES;
import static zipkin2.codec.SpanBytesEncoder.JSON_V2;

@Configuration
public class CapiConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CapiConfiguration.class);
    private final String tracesEndpoint;
    private final HttpUtils httpUtils;
    private final boolean capiTrustStoreEnabled;
    private final String capiTrustStorePath;
    private final String capiTrustStorePassword;
    private final CamelContext camelContext;
    private final ResourceLoader resourceLoader;
    private CapiTrustManager capiTrustManager;
    private final List<String> allowedHeaders;

    public CapiConfiguration(@Value("${capi.traces.endpoint}") String tracesEndpoint,
                             HttpUtils httpUtils,
                             CamelContext camelContext,
                             ResourceLoader resourceLoader,
                             @Value("${capi.trust.store.enabled}") boolean capiTrustStoreEnabled,
                             @Value("${capi.trust.store.path}") String capiTrustStorePath,
                             @Value("${capi.trust.store.password}") String capiTrustStorePassword,
                             @Value("${capi.gateway.cors.management.allowed-headers}") List<String> allowedHeaders) {

        this.tracesEndpoint = tracesEndpoint;
        this.httpUtils = httpUtils;
        this.camelContext = camelContext;
        this.resourceLoader = resourceLoader;
        this.capiTrustStoreEnabled = capiTrustStoreEnabled;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.allowedHeaders = allowedHeaders;

        if(capiTrustStoreEnabled) {
            createTrustMaterial();
        }

    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
    public Map<String, WebsocketClient> websocketClients() {
        return new HashMap<>();
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.traces", name = "enabled", havingValue = "true")
    CapiTracer capiTracer(CamelContext camelContext) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        camelContext.setUseMDCLogging(true);

        log.debug("Traces Enabled!");

        Set<String> excludePatterns = new HashSet<>();
        excludePatterns.add("timer://");
        excludePatterns.add("bean://consulNodeDiscovery");
        excludePatterns.add("bean://consistencyChecker");

        CapiTracer capiTracer = new CapiTracer(httpUtils);
        RestTemplateSender restTemplateSender = new RestTemplateSender(createRestTemplate(), tracesEndpoint, null, JSON_V2);

        capiTracer.setSpanReporter(AsyncReporter.builder(restTemplateSender).build());
        capiTracer.setIncludeMessageBody(true);
        capiTracer.setIncludeMessageBodyStreams(true);

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
    public RestTemplate createRestTemplate() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HttpClientConnectionManager httpClientConnectionManager;
        if(capiTrustStoreEnabled) {
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(getFile(), capiTrustStorePassword.toCharArray())
                    .build();

            SSLConnectionSocketFactory sslConFactory = new SSLConnectionSocketFactory(sslContext);
            httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder
                    .create()
                    .setSSLSocketFactory(sslConFactory)
                    .build();

        } else {
            httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder
                    .create()
                    .build();
        }
        CloseableHttpClient closeableHttpClient = HttpClients
                .custom()
                .setConnectionManager(httpClientConnectionManager)
                .build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(closeableHttpClient);
        return new RestTemplate(requestFactory);
    }

    @Bean
    public OkHttpClient httpClient()  {
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if(capiTrustStoreEnabled) {
                SSLContext sslContext = SSLContextBuilder
                        .create()
                        .loadTrustMaterial(getFile(), capiTrustStorePassword.toCharArray())
                        .build();
                builder.sslSocketFactory(sslContext.getSocketFactory(), capiTrustManager);
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "consistencyChecker")
    public ConsistencyChecker consistencyChecker(CamelContext camelContext,
                                                 RouteUtils routeUtils,
                                                 Cache<String, Service> serviceCache) {
        return new ConsistencyChecker(camelContext, routeUtils, serviceCache);
    }

    @Bean(name = "consulKVStore")
    @ConditionalOnProperty(prefix = "capi.consul.kv", name = "enabled", havingValue = "true")
    public ConsulKVStore consulKVStore(RestTemplate restTemplate,
                                       Cache<String, List<String>> corsHeadersCache,
                                       @Value("${capi.consul.hosts}") List<String> capiConsulHosts) {
        return new ConsulKVStore(restTemplate, corsHeadersCache, capiConsulHosts.get(0));
    }

    private void createSslContext() {
        try {
            log.info("Starting CAPI HTTP Components SSL Context");
            File filePath = getFile();

            HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("https");
            if(filePath != null) {
                CapiTrustManager capiTrustManager = new CapiTrustManager(filePath.getAbsolutePath(), capiTrustStorePassword);
                TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
                trustManagersParameters.setTrustManager(capiTrustManager);

                SSLContextParameters sslContextParameters = new SSLContextParameters();
                sslContextParameters.setTrustManagers(trustManagersParameters);
                sslContextParameters.createSSLContext(camelContext);
                httpComponent.setSslContextParameters(sslContextParameters);

            } else {
                log.warn("Could not create SSL Context, the provided certificate path is invalid");
            }

        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private File getFile() {
        File filePath = null;
        try {
            if(capiTrustStorePath.startsWith("classpath")) {
                Resource resource = resourceLoader.getResource(capiTrustStorePath);
                filePath = resource.getFile();
            } else {
                filePath = new File(capiTrustStorePath);
            }
        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
        return filePath;
    }

    private void createTrustMaterial() {
        try {
            File filePath = getFile();
            capiTrustManager = new CapiTrustManager(filePath.getAbsolutePath(), capiTrustStorePassword);
            createSslContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "capiCorsFilterStrategy")
    @ConditionalOnProperty(prefix = "capi.gateway.cors", name = "management.enabled", havingValue = "true")
    public CapiCorsFilterStrategy capiCorsFilterStrategy() {
        return new CapiCorsFilterStrategy(allowedHeaders);
    }

}