package io.surisoft.capi.throttle;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.surisoft.capi.schema.ThrottleServiceObject;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "capi.throttling", name = "enabled", havingValue = "true")
public class ThrottleCacheManager {

    public static final String CONSUMER_THROTTLE_CACHE = "consumer-throttle-cache";
    public static final String GLOBAL_THROTTLE_CACHE = "global-throttle-cache";
    private final HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(createConfig());

    private final String capiInstance;
    private final String capiKubeNamespace;

    public ThrottleCacheManager(@Value("${capi.namespace}") String capiInstance, @Value("${capi.kube.namespace}") String capiKubeNamespace) {
        this.capiInstance = capiInstance;
        this.capiKubeNamespace = capiKubeNamespace;
    }

    @PostConstruct
    public void addListener() {
        hazelcastInstance.getMap(GLOBAL_THROTTLE_CACHE).addEntryListener(new GlobalThrottleCacheListener(), true);
    }

    public void insertGlobal(String serviceId, long duration, ThrottleServiceObject throttleServiceObject) {
        IMap<String, ThrottleServiceObject> map = hazelcastInstance.getMap(GLOBAL_THROTTLE_CACHE);
        map.put(serviceId, throttleServiceObject, duration, TimeUnit.MILLISECONDS);
    }

    public void updateGlobal(String serviceId, long duration, ThrottleServiceObject throttleServiceObject) {
        IMap<String, ThrottleServiceObject> map = hazelcastInstance.getMap(GLOBAL_THROTTLE_CACHE);
        map.put(serviceId, throttleServiceObject, duration, TimeUnit.MILLISECONDS);
    }

    public ThrottleServiceObject getGlobal(String serviceId) {
        IMap<String, ThrottleServiceObject> map = hazelcastInstance.getMap(GLOBAL_THROTTLE_CACHE);
        return map.get(serviceId);
    }

    public void insertConsumer(String consumerId, long duration, ThrottleServiceObject throttleServiceObject) {
        IMap<String, ThrottleServiceObject> map = hazelcastInstance.getMap(CONSUMER_THROTTLE_CACHE);
        map.put(consumerId, throttleServiceObject, duration, TimeUnit.MILLISECONDS);
    }

    public void updateConsumer(String consumerId, long duration, ThrottleServiceObject throttleServiceObject) {
        IMap<String, ThrottleServiceObject> map = hazelcastInstance.getMap(CONSUMER_THROTTLE_CACHE);
        map.put(consumerId, throttleServiceObject, duration, TimeUnit.MILLISECONDS);
    }

    public ThrottleServiceObject getConsumer(String consumerId) {
        IMap<String, ThrottleServiceObject> map = hazelcastInstance.getMap(CONSUMER_THROTTLE_CACHE);
        return map.get(consumerId);
    }

    public Config createConfig() {
        Config config = new Config();
        config.setInstanceName(capiInstance);

        // Network config
        NetworkConfig network = config.getNetworkConfig();
        JoinConfig join = network.getJoin();

        if (capiKubeNamespace != null) {
            join.getMulticastConfig().setEnabled(false);
            join.getTcpIpConfig().setEnabled(false);
            join.getKubernetesConfig()
                    .setEnabled(true)
                    .setProperty("namespace", capiKubeNamespace)
                    .setProperty("service-name", "hazelcast-service");
        } else {
            join.getMulticastConfig().setEnabled(true);
            join.getTcpIpConfig().setEnabled(false);
        }

        config.addMapConfig(globalMapConfig());
        config.addMapConfig(consumerMapConfig());
        config.getJetConfig().setEnabled(true);
        return config;
    }

    private MapConfig globalMapConfig() {
        MapConfig mapConfig = new MapConfig(GLOBAL_THROTTLE_CACHE);
        mapConfig.setTimeToLiveSeconds(360);
        mapConfig.setMaxIdleSeconds(400);
        return mapConfig;
    }

    private MapConfig consumerMapConfig() {
        MapConfig mapConfig = new MapConfig(CONSUMER_THROTTLE_CACHE);
        mapConfig.setTimeToLiveSeconds(360);
        mapConfig.setMaxIdleSeconds(400);
        return mapConfig;
    }
}