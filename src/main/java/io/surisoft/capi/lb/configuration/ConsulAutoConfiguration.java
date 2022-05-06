package io.surisoft.capi.lb.configuration;

import io.surisoft.capi.lb.cache.ConsulCacheManager;
import io.surisoft.capi.lb.cache.ConsulDiscoveryCacheManager;
import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.processor.ConsulNodeDiscovery;
import io.surisoft.capi.lb.processor.MetricsProcessor;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.HttpUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsulAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConsulAutoConfiguration.class);

    @Value("${capi.consul.discovery.enabled}")
    private boolean consulEnabled;

    @Value("${capi.consul.discovery.timer.interval}")
    private int consulTimerInterval;

    @Value("${capi.consul.host}")
    private String capiConsulHost;

    @Value("${camel.servlet.mapping.context-path}")
    private String capiContext;

    @Bean(name = "consulNodeDiscovery")
    public ConsulNodeDiscovery consulNodeDiscovery(CamelContext camelContext, ApiUtils apiUtils, RouteUtils routeUtils, MetricsProcessor metricsProcessor, HttpUtils httpUtils, StickySessionCacheManager stickySessionCacheManager, ConsulCacheManager consulCacheManager) {
        return new ConsulNodeDiscovery(camelContext, capiConsulHost, apiUtils, routeUtils, metricsProcessor, stickySessionCacheManager, consulCacheManager, httpUtils.getCapiContext(capiContext));
    }

    @Bean
    public RouteBuilder routeBuilder(ConsulDiscoveryCacheManager consulDiscoveryCacheManager) {
        log.debug("Creating Capi Consul Discovery");
        if(consulDiscoveryCacheManager.getLocalMemberID().equals(consulDiscoveryCacheManager.getConsulWorkerNode().getMember()) && consulEnabled) {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("timer:consul-inspect?period=" + consulTimerInterval + "s")
                            .to("bean:consulNodeDiscovery?method=processInfo")
                            .routeId("consul-discovery-service");
                }
            };
        } else {
            return null;
        }
    }
}