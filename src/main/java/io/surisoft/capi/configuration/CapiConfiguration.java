package io.surisoft.capi.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxMeterRegistry;
import io.surisoft.capi.exception.RestTemplateErrorHandler;
import io.surisoft.capi.schema.ConsulKeyStoreEntry;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.*;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.RouteUtils;
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
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;

import static org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryNamingStrategy.MESSAGE_HISTORIES;
import static org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyNamingStrategy.ROUTE_POLICIES;

@Configuration
public class CapiConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CapiConfiguration.class);
    private final boolean capiTrustStoreEnabled;
    private final String capiTrustStorePath;
    private final String capiTrustStorePassword;
    private final String capiTrustStoreEncoded;
    private final CamelContext camelContext;
    private final ResourceLoader resourceLoader;
    private CapiTrustManager capiTrustManager;
    private final List<String> allowedHeaders;
    private final String sslPath;
    private final String sslPassword;
    private final boolean capiDisableRedirect;
    private final int consulTimerInterval;
    private final boolean capiConsulEnabled;
    private final Cache<String, Service> serviceCache;

    public CapiConfiguration(CamelContext camelContext,
                             ResourceLoader resourceLoader,
                             @Value("${capi.trust.store.enabled}") boolean capiTrustStoreEnabled,
                             @Value("${capi.trust.store.path}") String capiTrustStorePath,
                             @Value("${capi.trust.store.password}") String capiTrustStorePassword,
                             @Value("${capi.trust.store.encoded}") String capiTrustStoreEncoded,
                             @Value("${capi.gateway.cors.management.allowed-headers}") List<String> allowedHeaders,
                             @Value("${server.ssl.key-store}") String sslPath,
                             @Value("${server.ssl.key-store-password}") String sslPassword,
                             @Value("${capi.disable.redirect}") boolean capiDisableRedirect,
                             @Value("${capi.consul.discovery.timer.interval}") int consulTimerInterval,
                             @Value("${capi.consul.discovery.enabled}") boolean capiConsulEnabled,
                             Cache<String, Service> serviceCache) {


        this.camelContext = camelContext;
        this.resourceLoader = resourceLoader;
        this.capiTrustStoreEnabled = capiTrustStoreEnabled;
        this.capiTrustStorePath = capiTrustStorePath;
        this.capiTrustStorePassword = capiTrustStorePassword;
        this.capiTrustStoreEncoded = capiTrustStoreEncoded;
        this.allowedHeaders = allowedHeaders;
        this.sslPath = sslPath;
        this.sslPassword = sslPassword;
        this.capiDisableRedirect = capiDisableRedirect;
        this.consulTimerInterval = consulTimerInterval;
        this.capiConsulEnabled = capiConsulEnabled;
        this.serviceCache = serviceCache;

        if(capiTrustStoreEnabled) {
            capiTrustManager = createTrustMaterial();
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
    public RestTemplate createRestTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HttpClientConnectionManager httpClientConnectionManager;
        if(capiTrustStoreEnabled) {
            DefaultClientTlsStrategy sslConFactory = new DefaultClientTlsStrategy(capiSSLContext().getSslContext());
            httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder
                    .create()
                    .setTlsSocketStrategy(sslConFactory)
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

    @Bean(name = "consistencyChecker")
    public ConsistencyChecker consistencyChecker(CamelContext camelContext,
                                                 RouteUtils routeUtils,
                                                 Cache<String, Service> serviceCache) {
        return new ConsistencyChecker(camelContext, routeUtils, serviceCache);
    }

    @Bean(name = "capiSslContext")
    public CapiSslContextHolder capiSSLContext() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        log.info("CAPI SSL Context Holder for Dynamic SSL");
        if(capiTrustStoreEnabled) {
            TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> false;
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(capiTrustManager.getKeyStore(), trustStrategy)
                    .build();
            return new CapiSslContextHolder(sslContext);
        }
        return null;
    }

    @Bean(name = "consulKVStore")
    @ConditionalOnProperty(prefix = "capi.consul.kv", name = "enabled", havingValue = "true")
    public ConsulKVStore consulKVStore(RestTemplate restTemplate,
                                       Cache<String, List<String>> corsHeadersCache,
                                       @Value("${capi.consul.kv.host}") String consulKvHost,
                                       @Value("${capi.consul.kv.token}") String consulKvToken,
                                       @Value("${capi.trust.store.password}") String capiTrustStorePassword,
                                       RouteUtils routeUtils,
                                       ConsulNodeDiscovery consulNodeDiscovery,
                                       CapiSslContextHolder capiSslContextHolder,
                                       CamelContext camelContext,
                                       Optional<OpaService> opaService) {
        return new ConsulKVStore(restTemplate, corsHeadersCache, consulKvStoreSubscriptionGroupCache(), routeUtils, consulKvHost, consulKvToken, capiTrustStorePassword, consulNodeDiscovery, capiSslContextHolder, camelContext, opaService.orElse(null));
    }

    @Bean("sslContextParameters")
    @ConditionalOnProperty(prefix = "capi.trust.store", name = "enabled", havingValue = "true")
    public SSLContextParameters createSSLContextParameters() throws Exception {
        HttpComponent httpComponent = (HttpComponent) camelContext.getComponent("https");
        CapiTrustManager capiTrustManager;

        if(capiTrustStoreEncoded != null && !capiTrustStoreEncoded.isEmpty()) {
                InputStream trusStoreInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(capiTrustStoreEncoded.getBytes()));
                capiTrustManager = new CapiTrustManager(trusStoreInputStream, null, capiTrustStorePassword);
        } else {
                File filePath = getFile(capiTrustStorePath);
                capiTrustManager = new CapiTrustManager(null, filePath.getAbsolutePath(), capiTrustStorePassword);
        }

        TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
        trustManagersParameters.setTrustManager(capiTrustManager);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);
        sslContextParameters.setSessionTimeout("1");

        sslContextParameters.createSSLContext(camelContext);
        httpComponent.setSslContextParameters(sslContextParameters);

        return sslContextParameters;
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

    private CapiTrustManager createTrustMaterial() {
        CapiTrustManager capiTrustManager;
        try {
            if(capiTrustStoreEncoded != null && !capiTrustStoreEncoded.isEmpty()) {
                InputStream trusStoreInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(capiTrustStoreEncoded.getBytes()));
                capiTrustManager = new CapiTrustManager(trusStoreInputStream, null, capiTrustStorePassword);
            } else {
                File filePath = getFile(capiTrustStorePath);
                capiTrustManager = new CapiTrustManager(null, filePath.getAbsolutePath(), capiTrustStorePassword);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return capiTrustManager;
    }

    @Bean(name = "capiCorsFilterStrategy")
    @ConditionalOnProperty(prefix = "capi.gateway.cors", name = "management.enabled", havingValue = "true")
    public CapiCorsFilterStrategy capiCorsFilterStrategy() {
        return new CapiCorsFilterStrategy(allowedHeaders);
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.oauth2.provider", name = "enabled", havingValue = "true")
    public List<DefaultJWTProcessor<SecurityContext>> getJwtProcessor(Optional<CapiSslContextHolder> capiSslContextHolder) throws IOException, ParseException {
        log.trace("Starting CAPI JWT Processor");
        List<DefaultJWTProcessor<SecurityContext>> jwtProcessorList = new ArrayList<>();
        for(String jwkEndpoint : getOauth2ProviderKeys()) {
            HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
            capiSslContextHolder.ifPresent(sslContextHolder -> httpClientBuilder.sslContext(sslContextHolder.getSslContext()));
            httpClientBuilder.connectTimeout(Duration.ofSeconds(10));
            try {
                HttpClient httpClient = httpClientBuilder.build();
                HttpRequest request = HttpRequest.newBuilder().uri(new URI(jwkEndpoint)).build();
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == 200) {
                    InputStream responseInputStream = response.body();
                    DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
                    JWKSet jwkSet = JWKSet.load(responseInputStream);
                    ImmutableJWKSet<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
                    JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
                    JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
                    jwtProcessor.setJWSKeySelector(keySelector);
                    jwtProcessorList.add(jwtProcessor);
                }
            } catch (URISyntaxException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return jwtProcessorList;
    }

    @Bean
    @ConfigurationProperties( prefix = "capi.oauth2.provider.keys" )
    public List<String> getOauth2ProviderKeys(){
        return new ArrayList<>();
    }
}