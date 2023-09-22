package io.surisoft.capi.cache;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.StickySession;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Bean
    public Cache<String, Service> serviceCache() {
        log.debug("Creating Service Cache");
        return new Cache2kBuilder<String, Service>(){}
                .name("serviceCache-" + hashCode())
                .eternal(true)
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    @Bean
    public Cache<String, StickySession> stickySessionCache() {
        log.debug("Creating Service Cache");
        return new Cache2kBuilder<String, StickySession>(){}
                .name("stickySessionCache-" + hashCode())
                .eternal(true)
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }
}