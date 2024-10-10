package io.surisoft.capi.service;

import io.surisoft.capi.schema.ConsulKeyValueStore;
import io.surisoft.capi.utils.Constants;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class ConsulKVStore {

    private static final Logger log = LoggerFactory.getLogger(ConsulKVStore.class);
    private final RestTemplate restTemplate;
    private final Cache<String, List<String>> corsHeadersCache;
    private final String consulHost;

    public ConsulKVStore(RestTemplate restTemplate, Cache<String, List<String>> corsHeadersCache, String consulHost) {
        this.restTemplate = restTemplate;
        this.corsHeadersCache = corsHeadersCache;
        this.consulHost = consulHost;
    }

    public void process() {
        log.debug("Looking for key values...");
        capiCorsHeadersKVCall();
    }

    private void capiCorsHeadersKVCall() {
        List<String> cachedValueAsList = corsHeadersCache.get("capi-cors-headers");
        try {
            ResponseEntity<ConsulKeyValueStore[]> consulKeyValueStoreResponse = restTemplate.getForEntity(consulHost + Constants.CONSUL_KV_STORE_API + Constants.CAPI_CORS_HEADERS_CACHE_KEY, ConsulKeyValueStore[].class);
            if(consulKeyValueStoreResponse.getStatusCode().is2xxSuccessful()) {
                ConsulKeyValueStore consulKeyValueStore = Objects.requireNonNull(consulKeyValueStoreResponse.getBody())[0];
                List<String> consulDecodedValueAsList = consulKeyValueToList(consulKeyValueStore.getValue());
                if(cachedValueAsList != null && !consulDecodedValueAsList.isEmpty() && !cachedValueAsList.isEmpty() && !consulDecodedValueAsList.equals(cachedValueAsList)) {
                    log.debug("Cached Values Are different we need to save them to Consul KV");
                    updateConsulHeaderKey(Constants.CAPI_CORS_HEADERS_CACHE_KEY, String.join(",", cachedValueAsList));
                } else {
                    log.debug("Go to sleep and try later on...");
                }
            }
        } catch(Exception e) {
            log.debug("Cached Values Are different we need to save them to Consul KV");
            assert cachedValueAsList != null;
            updateConsulHeaderKey("capi-cors-headers", String.join(",", cachedValueAsList));
        }
    }

    private void updateConsulHeaderKey(String key, String value) {
        HttpEntity<String> request = new HttpEntity<>(value);
        restTemplate.put(consulHost + Constants.CONSUL_KV_STORE_API + key, request);
    }

    private List<String> consulKeyValueToList(String encodedValue) {
        String decodedValue = new String(Base64.getDecoder().decode(encodedValue));
        return Arrays.asList(decodedValue.split(",", -1));
    }
}