package io.surisoft.capi.configuration;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.service.ConsulNodeDiscovery;
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

import java.util.Map;

@Configuration
public class ConsulAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulAutoConfiguration.class);

    @Value("${capi.consul.discovery.timer.interval}")
    private int consulTimerInterval;

    @Value("${capi.consul.host}")
    private String capiConsulHost;

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

    @Bean(name = "consulNodeDiscovery")
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public ConsulNodeDiscovery consulNodeDiscovery(CamelContext camelContext,
                                                   ServiceUtils serviceUtils,
                                                   RouteUtils routeUtils,
                                                   MetricsProcessor metricsProcessor,
                                                   HttpUtils httpUtils,
                                                   StickySessionCacheManager stickySessionCacheManager,
                                                   Cache<String, Service> serviceCache) {
        camelContext.getRestConfiguration().setInlineRoutes(true);
        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, serviceUtils, routeUtils, metricsProcessor, stickySessionCacheManager, serviceCache, websocketClientMap);
        consulNodeDiscovery.setWebsocketUtils(websocketUtils);
        consulNodeDiscovery.setCapiContext(httpUtils.getCapiContext(capiContext));
        consulNodeDiscovery.setConsulHost(capiConsulHost);

        if(reverseProxyEnabled) {
            consulNodeDiscovery.setReverseProxyHost(reverseProxyHost);
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