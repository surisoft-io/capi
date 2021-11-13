package io.surisoft.capi.lb.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.surisoft.capi.lb.schema.ConsulWorkerNode;
import io.surisoft.capi.lb.schema.StickySession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class ConsulDiscoveryCacheManager {

    private HazelcastInstance hazelcastInstance;

    public ConsulDiscoveryCacheManager(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    private IMap<String, ConsulWorkerNode> getConsulWorkerNodeFromCache() {
        return hazelcastInstance.getMap(CacheConstants.CONSUL_WORKER_NODE_IMAP_NAME);
    }

    public String getLocalMemberID() {
        return hazelcastInstance.getCluster().getLocalMember().getUuid().toString();
    }

    public ConsulWorkerNode getConsulWorkerNode() {
        return getConsulWorkerNodeFromCache().get(CacheConstants.CONSUL_WORKER_NODE_ID);
    }

    @PostConstruct
    public void createWorkerNode() {
        ConsulWorkerNode consulWorkerNode = getConsulWorkerNodeFromCache().get(CacheConstants.CONSUL_WORKER_NODE_ID);
        if(consulWorkerNode == null) {
            consulWorkerNode = new ConsulWorkerNode();
            consulWorkerNode.setMember(this.hazelcastInstance.getCluster().getLocalMember().getUuid().toString());
            getConsulWorkerNodeFromCache().put(CacheConstants.CONSUL_WORKER_NODE_ID, consulWorkerNode);
        }
        log.info("Consul Worker Node ID: {}", consulWorkerNode.getMember());
    }
}