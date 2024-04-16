package io.surisoft.capi.configuration;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.ConsulNodeDiscovery;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.HttpUtils;
import io.surisoft.capi.utils.RouteUtils;
import io.surisoft.capi.utils.ServiceUtils;
import io.surisoft.capi.utils.WebsocketUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Configuration
public class ConsulAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulAutoConfiguration.class);
    private final int consulTimerInterval;

    private final String capiConsulHosts;

    private final String consulToken;

    private final String capiContext;

    private final boolean reverseProxyEnabled;

    private final String reverseProxyHost;

    private final Map<String, WebsocketClient> websocketClientMap;

    private final WebsocketUtils websocketUtils;

    private final Optional<StickySessionCacheManager> stickySessionCacheManager;

    private final Optional<OpaService> opaService;

    private final String capiNamespace;

    public ConsulAutoConfiguration(@Value("${capi.consul.discovery.timer.interval}") int consulTimerInterval,
                                   @Value("${capi.consul.hosts}") String capiConsulHosts,
                                   @Value("${capi.consul.token}") String consulToken,
                                   @Value("${camel.servlet.mapping.context-path}") String capiContext,
                                   @Value("${capi.reverse.proxy.enabled}") boolean reverseProxyEnabled,
                                   @Value("${capi.reverse.proxy.host}") String reverseProxyHost,
                                   Map<String, WebsocketClient> websocketClientMap,
                                   WebsocketUtils websocketUtils,
                                   Optional<StickySessionCacheManager> stickySessionCacheManager,
                                   Optional<OpaService> opaService,
                                   @Value("${capi.namespace}") String capiNamespace) {
        this.consulTimerInterval = consulTimerInterval;
        this.capiConsulHosts = capiConsulHosts;
        this.consulToken = consulToken;
        this.capiContext = capiContext;
        this.reverseProxyEnabled = reverseProxyEnabled;
        this.reverseProxyHost = reverseProxyHost;
        this.websocketClientMap = websocketClientMap;
        this.websocketUtils = websocketUtils;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.opaService = opaService;
        this.capiNamespace = capiNamespace;
    }

    @Bean(name = "consulNodeDiscovery")
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public ConsulNodeDiscovery consulNodeDiscovery(CamelContext camelContext,
                                                   ServiceUtils serviceUtils,
                                                   RouteUtils routeUtils,
                                                   MetricsProcessor metricsProcessor,
                                                   HttpUtils httpUtils,
                                                   Cache<String, Service> serviceCache) {

        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, serviceUtils, routeUtils, metricsProcessor, serviceCache, websocketClientMap);
        consulNodeDiscovery.setHttpUtils(httpUtils);

        opaService.ifPresent(consulNodeDiscovery::setOpaService);
        consulNodeDiscovery.setWebsocketUtils(websocketUtils);
        consulNodeDiscovery.setCapiContext(httpUtils.getCapiContext(capiContext));
        consulNodeDiscovery.setConsulHostList(Arrays.asList(capiConsulHosts.split("\\s*,\\s*")));
        if(capiNamespace != null && !capiNamespace.isEmpty()) {
            consulNodeDiscovery.setCapiNamespace(capiNamespace);
        }

        if(consulToken != null && !consulToken.isEmpty()) {
            consulNodeDiscovery.setConsulToken(consulToken);
        }

        if(reverseProxyEnabled) {
            consulNodeDiscovery.setReverseProxyHost(reverseProxyHost);
        }

        stickySessionCacheManager.ifPresent(consulNodeDiscovery::setStickySessionCacheManager);
        return consulNodeDiscovery;
    }

    @Bean
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
    }
}