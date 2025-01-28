package io.surisoft.capi.configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxMeterRegistry;
import io.surisoft.capi.exception.RestTemplateErrorHandler;
import io.surisoft.capi.schema.*;
import io.surisoft.capi.service.CapiTrustManager;
import io.surisoft.capi.service.ConsistencyChecker;
import io.surisoft.capi.service.ConsulKVStore;
import io.surisoft.capi.tracer.CapiTracer;
import io.surisoft.capi.tracer.CapiUndertowTracer;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import io.surisoft.capi.utils.RouteUtils;
import io.surisoft.scim.ScimController;
import io.surisoft.scim.resources.ControllerConfiguration;
import okhttp3.OkHttpClient;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.component.micrometer.CamelJmxConfig;
import org.apache.camel.component.micrometer.DistributionStatisticConfigFilter;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy.MESSAGE_HISTORIES;
import static org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy.ROUTE_POLICIES;

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
    private final String capiScimImplementationPath;
    private final Environment environment;
    private final String sslPath;
    private final String sslPassword;
    private final boolean capiDisableRedirect;
    private final int consulTimerInterval;
    private final boolean capiConsulEnabled;
    private final Cache<String, Service> serviceCache;
    private final String consulKvHost;

    public CapiConfiguration(@Value("${capi.traces.endpoint}") String tracesEndpoint,
                             HttpUtils httpUtils,
                             CamelContext camelContext,
                             ResourceLoader resourceLoader,
                             @Value("${capi.trust.store.enabled}") boolean capiTrustStoreEnabled,
                             @Value("${capi.trust.store.path}") String capiTrustStorePath,
                             @Value("${capi.trust.store.password}") String capiTrustStorePassword,
                             @Value("${capi.gateway.cors.management.allowed-headers}") List<String> allowedHeaders,
                             @Value("${capi.scim.implementation.path}") String capiScimImplementationPath,
                             Environment environment,
                             @Value("${server.ssl.key-store}") String sslPath,
                             @Value("${server.ssl.key-store-password}") String sslPassword,
                             @Value("${capi.disable.redirect}") boolean capiDisableRedirect,
                             @Value("${capi.consul.discovery.timer.interval}") int consulTimerInterval,
                             @Value("${capi.consul.discovery.enabled}") boolean capiConsulEnabled,
                             Cache<String, Service> serviceCache,
                             @Value("${capi.consul.kv.host}") String consulKvHost) {


        this.tracesEndpoint = tracesEndpoint;
        this.httpUtils = httpUtils;
        this.camelContext = camelContext;
        this.resourceLoader = resourceLoader;
        this.capiTrustStoreEnabled = capiTrustStoreEnabled;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.allowedHeaders = allowedHeaders;
        this.capiScimImplementationPath = capiScimImplementationPath;
        this.environment = environment;
        this.sslPath = sslPath;
        this.sslPassword = sslPassword;
        this.capiDisableRedirect = capiDisableRedirect;
        this.consulTimerInterval = consulTimerInterval;
        this.capiConsulEnabled = capiConsulEnabled;
        this.serviceCache = serviceCache;
        this.consulKvHost = consulKvHost;

        if(capiTrustStoreEnabled) {
            createTrustMaterial();
        }

    }

    @Bean
    public CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                try {
                    log.debug("Initializing CamelContext Startup Listener");
                    camelContext.addStartupListener(new CamelStartupListener(consulTimerInterval, capiConsulEnabled));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {}
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.scim", name = "enabled", havingValue = "true")
    public ScimController scimController() {
        AtomicReference<ScimController> scimController = new AtomicReference<>();
        if(canCreateScimController()) {
            log.info("CAPI SCIM, looking for a provided implementation.");
            try {
                URL jarUrl = Path.of(capiScimImplementationPath).toUri().toURL();
                try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { jarUrl }, CapiConfiguration.class.getClassLoader())) {
                    ServiceLoader<ScimController> serviceLoader = ServiceLoader.load(ScimController.class, urlClassLoader);
                    serviceLoader.iterator().forEachRemaining(scimControllerEntry -> {
                        log.info("Found SCIM Implementation: {}", scimControllerEntry.getClass().getName());
                        ControllerConfiguration scimControllerConfiguration = new ControllerConfiguration();
                        scimControllerConfiguration.setUsername(environment.getProperty("scim-controller-auth-username"));
                        scimControllerConfiguration.setPassword(environment.getProperty("scim-controller-auth-password"));
                        scimControllerConfiguration.setBaseUrl(environment.getProperty("scim-controller-base-url"));
                        scimControllerConfiguration.setBasePath(environment.getProperty("scim-controller-base-path"));
                        scimControllerEntry.init(scimControllerConfiguration);
                        scimController.set(scimControllerEntry);
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.info("SCIM is enabled but scim properties were not found in the environment variables");
            log.info("The following properties are mandatory: {}", "scim-controller-auth-username, scim-controller-auth-password, scim-controller-base-url, scim-controller-base-path");
            return null;
        }
        return scimController.get();
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.websocket", name = "enabled", havingValue = "true")
    public Map<String, WebsocketClient> websocketClients() {
        return new HashMap<>();
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.sse", name = "enabled", havingValue = "true")
    public Map<String, SSEClient> sseClients() {
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
    @ConditionalOnProperty(prefix = "capi.traces", name = "enabled", havingValue = "true")
    CapiUndertowTracer capiUndertowTracer() throws Exception {
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

    @Bean
    public HttpComponent disableFollowRedirect(CamelContext camelContext) {
        HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("http");

        HttpClientConfigurer httpClientConfigurer = clientBuilder -> {
            if(capiDisableRedirect) {
                clientBuilder.disableAuthCaching();
            }
            clientBuilder.setConnectionReuseStrategy((request, response, context) -> false);
        };

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
                    .loadTrustMaterial(getFile(capiTrustStorePath), capiTrustStorePassword.toCharArray())
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
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(new RestTemplateErrorHandler());
        return restTemplate;
    }

    @Bean
    public OkHttpClient httpClient()  {
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if(capiTrustStoreEnabled) {
                SSLContext sslContext = SSLContextBuilder
                        .create()
                        .loadTrustMaterial(getFile(capiTrustStorePath), capiTrustStorePassword.toCharArray())
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
        return new ConsulKVStore(restTemplate, corsHeadersCache, consulKvStoreSubscriptionGroupCache(), serviceCache, httpUtils, capiConsulHosts.get(0), consulKvHost);
    }

    private void createSslContext() {
        try {
            log.info("Starting CAPI HTTP Components SSL Context");
            File filePath = getFile(capiTrustStorePath);

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

    @Bean
    @ConditionalOnProperty(prefix = "server.ssl", name = "enabled", havingValue = "true")
    public SSLContext createSSLContextForUndertow() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        File filePath = getFile(sslPath);
        if(filePath != null) {
            return new SSLContextBuilder().loadKeyMaterial(filePath, sslPassword.toCharArray(), sslPassword.toCharArray()).build();
        }
        return null;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.consul.kv", name = "enabled", havingValue = "true")
    public Cache<String, ConsulKeyStoreEntry> consulKvStoreSubscriptionGroupCache() {
        log.debug("Creating Subscription Group Cache");
        return new Cache2kBuilder<String, ConsulKeyStoreEntry>(){}
                .name("consulSubscriptionGroupCache-" + hashCode())
                .eternal(true)
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    private File getFile(String path) {
        File filePath = null;
        try {
            if(path.startsWith("classpath")) {
                Resource resource = resourceLoader.getResource(path);
                filePath = resource.getFile();
            } else {
                filePath = new File(path);
            }
        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
        return filePath;
    }

    private void createTrustMaterial() {
        try {
            File filePath = getFile(capiTrustStorePath);
            capiTrustManager = new CapiTrustManager(filePath.getAbsolutePath(), capiTrustStorePassword);
            createSslContext();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "capiCorsFilterStrategy")
    @ConditionalOnProperty(prefix = "capi.gateway.cors", name = "management.enabled", havingValue = "true")
    public CapiCorsFilterStrategy capiCorsFilterStrategy() {
        return new CapiCorsFilterStrategy(allowedHeaders);
    }

    private boolean canCreateScimController() {
        return environment.containsProperty("scim-controller-auth-username") &&
                environment.containsProperty("scim-controller-auth-password") &&
                environment.containsProperty("scim-controller-base-url") &&
                environment.containsProperty("scim-controller-base-path");
    }
}