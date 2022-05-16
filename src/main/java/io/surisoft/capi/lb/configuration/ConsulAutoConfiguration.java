package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.MetricsProcessor;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.service.ConsulNodeDiscovery;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.HttpUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsulAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulAutoConfiguration.class);

    @Value("${capi.consul.discovery.timer.interval}")
    private int consulTimerInterval;

    @Value("${capi.consul.host}")
    private String capiConsulHost;

    @Value("${camel.servlet.mapping.context-path}")
    private String capiContext;

    @Bean(name = "consulNodeDiscovery")
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public ConsulNodeDiscovery consulNodeDiscovery(CamelContext camelContext, ApiUtils apiUtils, RouteUtils routeUtils, MetricsProcessor metricsProcessor, HttpUtils httpUtils, StickySessionCacheManager stickySessionCacheManager, Cache<String, Api> apiCache) {
        ConsulNodeDiscovery consulNodeDiscovery = new ConsulNodeDiscovery(camelContext, apiUtils, routeUtils, metricsProcessor, stickySessionCacheManager, apiCache);
        consulNodeDiscovery.setCapiContext(httpUtils.getCapiContext(capiContext));
        consulNodeDiscovery.setConsulHost(capiConsulHost);
        return consulNodeDiscovery;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.consul.discovery", name = "enabled", havingValue = "true")
    public RouteBuilder routeBuilder() {
        log.debug("Creating Capi Consul Node Discovery");
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:consul-inspect?period=" + consulTimerInterval + "s")
                        .to("bean:consulNodeDiscovery?method=processInfo")
                        .routeId("consul-discovery-service");
            }
        };
    }
}