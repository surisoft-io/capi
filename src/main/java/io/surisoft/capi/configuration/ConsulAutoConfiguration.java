package io.surisoft.capi.configuration;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.ContentTypeValidator;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.ConsulNodeDiscovery;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.*;
import org.apache.camel.CamelContext;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class ConsulAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulAutoConfiguration.class);
    //private final int consulTimerInterval;

    private final List<String> capiConsulHosts;

    private final String consulToken;

    private final String capiContext;

    private final boolean reverseProxyEnabled;

    private final String reverseProxyHost;

    private final Map<String, WebsocketClient> websocketClientMap;

    private final Map<String, SSEClient> sseClientMap;

    private final WebsocketUtils websocketUtils;

    private final SSEUtils sseUtils;

    private final Optional<StickySessionCacheManager> stickySessionCacheManager;

    private final Optional<OpaService> opaService;

    private final String capiNamespace;
    private final boolean strictNamespace;
    private final String capiRunningMode;
    private final ContentTypeValidator contentTypeValidator;

    public ConsulAutoConfiguration(@Value("${capi.consul.hosts}") List<String> capiConsulHosts,
                                   @Value("${capi.consul.token}") String consulToken,
                                   @Value("${camel.servlet.mapping.context-path}") String capiContext,
                                   @Value("${capi.reverse.proxy.enabled}") boolean reverseProxyEnabled,
                                   @Value("${capi.reverse.proxy.host}") String reverseProxyHost,
                                   Map<String, WebsocketClient> websocketClientMap,
                                   Map<String, SSEClient> sseClientMap,
                                   WebsocketUtils websocketUtils,
                                   SSEUtils sseUtils,
                                   Optional<StickySessionCacheManager> stickySessionCacheManager,
                                   Optional<OpaService> opaService,
                                   @Value("${capi.namespace}") String capiNamespace,
                                   @Value("${capi.strict}") boolean strictNamespace,
                                   @Value("${capi.mode}") String capiRunningMode,
                                   ContentTypeValidator contentTypeValidator) {
        this.capiConsulHosts = capiConsulHosts;
        this.consulToken = consulToken;
        this.capiContext = capiContext;
        this.reverseProxyEnabled = reverseProxyEnabled;
        this.reverseProxyHost = reverseProxyHost;
        this.websocketClientMap = websocketClientMap;
        this.sseClientMap = sseClientMap;
        this.websocketUtils = websocketUtils;
        this.sseUtils = sseUtils;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.opaService = opaService;
        this.capiNamespace = capiNamespace;
        this.strictNamespace = strictNamespace;
        this.capiRunningMode = capiRunningMode;
        this.contentTypeValidator = contentTypeValidator;
    }

    @Bean(name = "consulNodeDiscovery")
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public ConsulNodeDiscovery consulNodeDiscovery(CamelContext camelContext,
                                                   ServiceUtils serviceUtils,
                                                   RouteUtils routeUtils,
                                                   MetricsProcessor metricsProcessor,
                                                   HttpUtils httpUtils,
                                                   Cache<String, Service> serviceCache) {

        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, serviceUtils, routeUtils, metricsProcessor, serviceCache, websocketClientMap, sseClientMap, contentTypeValidator);
        consulNodeDiscovery.setHttpUtils(httpUtils);

        opaService.ifPresent(consulNodeDiscovery::setOpaService);
        consulNodeDiscovery.setWebsocketUtils(websocketUtils);
        consulNodeDiscovery.setSSEUtils(sseUtils);
        consulNodeDiscovery.setCapiContext(httpUtils.getCapiContext(capiContext));
        consulNodeDiscovery.setConsulHostList(capiConsulHosts);
        if(capiNamespace != null && !capiNamespace.isEmpty()) {
            consulNodeDiscovery.setCapiNamespace(capiNamespace);
            consulNodeDiscovery.setStrictNamespace(strictNamespace);
        }

        if(consulToken != null && !consulToken.isEmpty()) {
            consulNodeDiscovery.setConsulToken(consulToken);
        }

        if(reverseProxyEnabled) {
            consulNodeDiscovery.setReverseProxyHost(reverseProxyHost);
        }

        consulNodeDiscovery.setCapiRunningMode(capiRunningMode);

        stickySessionCacheManager.ifPresent(consulNodeDiscovery::setStickySessionCacheManager);
        return consulNodeDiscovery;
    }

    /*@Bean
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public RouteBuilder routeBuilder() {
        log.debug("Creating Capi Consul Node Discovery");
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:consul-inspect?period=" + consulTimerInterval)
                        .to("bean:consulNodeDiscovery?method=processInfo")
                        .routeId("consul-discovery-service");
            }
        };
    }*/
}