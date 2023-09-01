package io.surisoft.capi.cache;

import io.surisoft.capi.schema.Api;
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
                .eternal(true)
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }
}