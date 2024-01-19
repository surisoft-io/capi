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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;

@Configuration
public class ConsulAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulAutoConfiguration.class);

    @Value("${capi.consul.discovery.timer.interval}")
    private int consulTimerInterval;

    @Value("${capi.consul.hosts}")
    private String capiConsulHosts;

    @Value("${camel.servlet.mapping.context-path}")
    private String capiContext;

    @Value("${capi.reverse.proxy.enabled}")
    private boolean reverseProxyEnabled;

    @Value("${capi.reverse.proxy.host}")
    private String reverseProxyHost;

    @Autowired(required = false)
    private Map<String, WebsocketClient> websocketClientMap;

    @Autowired
    private WebsocketUtils websocketUtils;

    @Autowired(required = false)
    private StickySessionCacheManager stickySessionCacheManager;

    @Autowired(required = false)
    private OpaService opaService;

    @Value("${capi.namespace}")
    private String capiNamespace;

    @Bean(name = "consulNodeDiscovery")
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public ConsulNodeDiscovery consulNodeDiscovery(CamelContext camelContext,
                                                   ServiceUtils serviceUtils,
                                                   RouteUtils routeUtils,
                                                   MetricsProcessor metricsProcessor,
                                                   HttpUtils httpUtils,
                                                   Cache<String, Service> serviceCache) {
        camelContext.getRestConfiguration().setInlineRoutes(true);
        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, serviceUtils, routeUtils, metricsProcessor, serviceCache, websocketClientMap);
        consulNodeDiscovery.setHttpUtils(httpUtils);
        consulNodeDiscovery.setOpaService(opaService);
        consulNodeDiscovery.setWebsocketUtils(websocketUtils);
        consulNodeDiscovery.setCapiContext(httpUtils.getCapiContext(capiContext));
        consulNodeDiscovery.setConsulHostList(Arrays.asList(capiConsulHosts.split("\\s*,\\s*")));
        if(capiNamespace != null && !capiNamespace.isEmpty()) {
            consulNodeDiscovery.setCapiNamespace(capiNamespace);
        }

        if(reverseProxyEnabled) {
            consulNodeDiscovery.setReverseProxyHost(reverseProxyHost);
        }

        if(stickySessionCacheManager != null) {
            consulNodeDiscovery.setStickySessionCacheManager(stickySessionCacheManager);
        }

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