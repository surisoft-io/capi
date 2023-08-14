package io.surisoft.capi.cache;

import com.hazelcast.map.IMap;
import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.utils.RouteUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StickySessionCacheManager {

    private RouteUtils routeUtils;
    //private Cache<String, StickySession> stickySessionCache;
    @Autowired
    private IMap<String, StickySession> stickySessionCache;

    public StickySessionCacheManager(RouteUtils routeUtils) {
        //this.stickySessionCache = stickySessionCache;
        this.routeUtils = routeUtils;
    }

    public void createStickySession(StickySession stickySession) {
        stickySessionCache.put(stickySession.getId(), stickySession);
        //stickySessionCache.put(stickySession.getId(), stickySession);
    }

    public StickySession getStickySessionById(String paramName, String paramValue) {
        String stickySessionId = routeUtils.getStickySessionId(paramName, paramValue);
        if(stickySessionCache.containsKey(stickySessionId)) {
            return stickySessionCache.get(stickySessionId);
        }
        return null;
    }

    public void deleteStickySession(StickySession stickySession) {
        //stickySessionCache.remove(stickySession.getId());
        stickySessionCache.remove(stickySession.getId());
    }
}