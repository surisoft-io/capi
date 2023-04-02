package io.surisoft.capi.lb.cache;

import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.StickySession;
import io.surisoft.capi.lb.utils.Constants;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Value("${sticky.session.time.to.live}")
    private Integer stickySessionTimeToLive;

    @Bean
    public Cache<String, Api> apiCache() {
        log.debug("Creating API Cache");
        return new Cache2kBuilder<String, Api>(){}
                .name("apiCache-" + hashCode())
                .eternal(true)
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    @Bean
    public Cache<String, StickySession> stickySessionCache() {
        log.debug("Creating Sticky Session Cache");
        return new Cache2kBuilder<String, StickySession>(){}
                .name("stickySession-" + hashCode())
                .expireAfterWrite(stickySessionTimeToLive, TimeUnit.HOURS)
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    @Bean
    public Cache<String, String> startRouteStoppedEventCache() {
        log.debug("Creating RouteStoppedEvent Cache");
        return new Cache2kBuilder<String, String>(){}
                .name(Constants.CACHE_ROUTE_STOPPED_EVENT + "-" + hashCode())
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    @Bean
    public Cache<String, Integer> startRouteRemovedEventCache() {
        log.debug("Creating RouteRemovedEvent Cache");
        return new Cache2kBuilder<String, Integer>(){}
                .name(Constants.CACHE_ROUTE_REMOVED_EVENT + "-" + hashCode())
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    @Bean
    public Cache<String, Integer> startExchangeFailedEventCache() {
        log.debug("Creating ExchangeFailedEvent Cache");
        return new Cache2kBuilder<String, Integer>(){}
                .name(Constants.CACHE_EXCHANGE_FAILED_EVENT + "-" + hashCode())
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }
}