package io.surisoft.capi.lb.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.surisoft.capi.lb.schema.StickySession;
import io.surisoft.capi.lb.utils.RouteUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class StickySessionCacheManager {

    private HazelcastInstance hazelcastInstance;
    private RouteUtils routeUtils;

    @Value("${sticky.session.time.to.live}")
    private long stickySessionTimeToLive;

    public StickySessionCacheManager(HazelcastInstance hazelcastInstance, RouteUtils routeUtils) {
        this.hazelcastInstance = hazelcastInstance;
        this.routeUtils = routeUtils;
    }

    private IMap<String, StickySession> getCachedStickySessions() {
        return hazelcastInstance.getMap(CacheConstants.STICKY_SESSION_IMAP_NAME);
    }

    public void createStickySession(StickySession stickySession) {
        getCachedStickySessions().put(stickySession.getId(), stickySession, stickySessionTimeToLive, TimeUnit.HOURS);
    }

    public StickySession getStickySessionById(String paramName, String paramValue) {
        return getCachedStickySessions().get(routeUtils.getStickySessionId(paramName, paramValue));
    }

    public void deleteStickySession(StickySession stickySession) {
        getCachedStickySessions().remove(stickySession.getId());
    }
}
