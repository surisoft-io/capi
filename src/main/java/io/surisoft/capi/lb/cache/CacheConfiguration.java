package io.surisoft.capi.lb.cache;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
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
import org.springframework.context.annotation.DependsOn;

@Configuration
public class CacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Value("${sticky.session.time.to.live}")
    private Integer stickySessionTimeToLive;

    @Value("${capi.kubernetes}")
    private boolean runningOnKubernetes;

    @Bean
    public Config hazelCastConfig() {
        Config config = new Config();
        config.setProperty("service-name", "capi");
        config.setInstanceName("capi");
        config.setClusterName("capi");



        //System.setProperty( "hazelcast.logging.type", "none" );

        if(runningOnKubernetes) {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getKubernetesConfig().setEnabled(true);
        }
        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        return Hazelcast.getOrCreateHazelcastInstance(hazelCastConfig());
    }

    @Bean
    @DependsOn("hazelcastInstance")
    public IMap<String, StickySession> getStickySessionCache(HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("StickySession");
    }

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

    //@Bean
    public Cache<String, String> startRouteStoppedEventCache() {
        log.debug("Creating RouteStoppedEvent Cache");
        return new Cache2kBuilder<String, String>(){}
                .name(Constants.CACHE_ROUTE_STOPPED_EVENT + "-" + hashCode())
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    //@Bean
    public Cache<String, Integer> startRouteRemovedEventCache() {
        log.debug("Creating RouteRemovedEvent Cache");
        return new Cache2kBuilder<String, Integer>(){}
                .name(Constants.CACHE_ROUTE_REMOVED_EVENT + "-" + hashCode())
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }

    //@Bean
    public Cache<String, Integer> startExchangeFailedEventCache() {
        log.debug("Creating ExchangeFailedEvent Cache");
        return new Cache2kBuilder<String, Integer>(){}
                .name(Constants.CACHE_EXCHANGE_FAILED_EVENT + "-" + hashCode())
                .entryCapacity(10000)
                .storeByReference(true)
                .build();
    }
}