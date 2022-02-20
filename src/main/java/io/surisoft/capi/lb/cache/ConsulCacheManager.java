package io.surisoft.capi.lb.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.surisoft.capi.lb.schema.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConsulCacheManager {

    private HazelcastInstance hazelcastInstance;

    public ConsulCacheManager(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public IMap<String, Api> getCachedApi() {
        return hazelcastInstance.getMap(CacheConstants.CONSUL_API_IMAP_NAME);
    }

    public void createApi(Api api) {
        getCachedApi().put(api.getId(), api);
    }

    public Api getApiById(String apiId) {
        return getCachedApi().get(apiId);
    }
}
