package io.surisoft.capi.metrics;

import org.cache2k.Cache;
import org.cache2k.CacheEntry;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "capi.consul.kv", name = "enabled", havingValue = "true")
@Endpoint(id = "kv")
public class KVStore {

    private final Cache<String, List<String>> corsHeadersCache;

    public KVStore(Cache<String, List<String>> corsHeadersCache) {
        this.corsHeadersCache = corsHeadersCache;
    }


    @ReadOperation
    public Set<CacheEntry<String, List<String>>> getCache() {
        return corsHeadersCache.entries();
    }

    @WriteOperation
    public List<String> addHeader(@Selector String key, @Selector String value) {
        List<String> headersCached = new ArrayList<>(Objects.requireNonNull(corsHeadersCache.get(key)));
        if(!headersCached.isEmpty()) {
            headersCached.add(value);
            corsHeadersCache.put(key, headersCached);
        }
        return headersCached;
    }

    @DeleteOperation
    public List<String> deleteHeader(@Selector String key, @Selector String value) {
        List<String> headersToKeep = new ArrayList<>();
        for(String header : Objects.requireNonNull(corsHeadersCache.get(key))) {
            if(!header.equals(value)) {
               headersToKeep.add(header);
            }
            corsHeadersCache.put(key, headersToKeep);
        }
        return headersToKeep;
    }
}