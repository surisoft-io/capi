package io.surisoft.capi.cache;

import io.surisoft.capi.schema.ConsulKeyValueStore;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.utils.Constants;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Configuration
public class CacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);
    private final List<String> allowedHeaders;
    private final List<String> capiConsulHosts;

    public CacheConfiguration(@Value("${capi.gateway.cors.management.allowed-headers}") List<String> allowedHeaders,
                              @Value("${capi.consul.hosts}") List<String> capiConsulHosts) {
        this.allowedHeaders = allowedHeaders;
        this.capiConsulHosts = capiConsulHosts;
    }


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

    @Bean
    @ConditionalOnProperty(prefix = "capi.consul.kv", name = "enabled", havingValue = "true")
    public Cache<String, List<String>> consulKvStoreCache(RestTemplate restTemplate) {
        String consulHost = capiConsulHosts.get(0);
        log.debug("Creating Consul KV Cache");
        Cache<String, List<String>> consulKvStoreCache = new Cache2kBuilder<String, List<String>>(){}
                .name("consulKvStoreCache-" + hashCode())
                .eternal(true)
                .entryCapacity(10000)
                .storeByReference(true)
                .build();

        //Processing CORS Headers
        log.info("Checking Consul Key Store for CORS Headers key/values");
        try {
            ResponseEntity<ConsulKeyValueStore[]> consulKeyValueStoreResponse = restTemplate.getForEntity(consulHost + Constants.CONSUL_KV_STORE_API + Constants.CAPI_CORS_HEADERS_CACHE_KEY, ConsulKeyValueStore[].class);
            if(!consulKeyValueStoreResponse.getStatusCode().is2xxSuccessful()) {
                consulKvStoreCache.put(Constants.CAPI_CORS_HEADERS_CACHE_KEY, allowedHeaders);
            } else {
                ConsulKeyValueStore consulKeyValueStore = Objects.requireNonNull(consulKeyValueStoreResponse.getBody())[0];
                List<String> consulDecodedValueAsList = consulKeyValueAsList(consulKeyValueStore.getValue());
                consulKvStoreCache.put(Constants.CAPI_CORS_HEADERS_CACHE_KEY, consulDecodedValueAsList);
            }
        } catch(Exception e) {
            consulKvStoreCache.put(Constants.CAPI_CORS_HEADERS_CACHE_KEY, allowedHeaders);
        }
        return consulKvStoreCache;
    }

    private List<String> consulKeyValueAsList(String encodedValue) {
        String decodedValue = new String(Base64.getDecoder().decode(encodedValue));
        return Arrays.asList(decodedValue.split(",", -1));
    }
}