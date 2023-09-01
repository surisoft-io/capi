package io.surisoft.capi.configuration;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.processor.MetricsProcessor;
import io.surisoft.capi.repository.ApiRepository;
import io.surisoft.capi.schema.Api;
import io.surisoft.capi.service.DBNodeDiscovery;
import io.surisoft.capi.utils.ApiUtils;
import io.surisoft.capi.utils.HttpUtils;
import io.surisoft.capi.utils.RouteUtils;
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
public class DBAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DBAutoConfiguration.class);

    @Value("${capi.db.discovery.timer.interval}")
    private int dbTimerInterval;

    @Value("${camel.servlet.mapping.context-path}")
    private String capiContext;

    @Bean(name = "dbNodeDiscovery")
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public DBNodeDiscovery dbNodeDiscovery(CamelContext camelContext, ApiUtils apiUtils, RouteUtils routeUtils, MetricsProcessor metricsProcessor, HttpUtils httpUtils, StickySessionCacheManager stickySessionCacheManager, ApiRepository apiRepository, Cache<String, Api> apiCache) {
        DBNodeDiscovery dbNodeDiscovery = new DBNodeDiscovery(camelContext, apiUtils, routeUtils, metricsProcessor, stickySessionCacheManager, apiCache);
        dbNodeDiscovery.setCapiContext(httpUtils.getCapiContext(capiContext));
        dbNodeDiscovery.setApiRepository(apiRepository);
        return dbNodeDiscovery;
    }

    @Bean
    @ConditionalOnProperty(prefix = "capi.persistence", name = "enabled", havingValue = "true")
    public RouteBuilder routeBuilder() {
        log.debug("Creating Capi DB Node Discovery");
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:db-inspect?period=" + dbTimerInterval)
                        .to("bean:dbNodeDiscovery?method=processInfo")
                        .routeId("db-discovery-service");
            }
        };
    }
}