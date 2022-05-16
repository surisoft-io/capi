package io.surisoft.capi.lb.cache;

import io.surisoft.capi.lb.schema.StickySession;
import io.surisoft.capi.lb.utils.RouteUtils;
import org.cache2k.Cache;
import org.springframework.stereotype.Component;

@Component
public class StickySessionCacheManager {

    private RouteUtils routeUtils;
    private Cache<String, StickySession> stickySessionCache;

    public StickySessionCacheManager(Cache<String, StickySession> stickySessionCache, RouteUtils routeUtils) {
        this.stickySessionCache = stickySessionCache;
        this.routeUtils = routeUtils;
    }

    public void createStickySession(StickySession stickySession) {
        stickySessionCache.put(stickySession.getId(), stickySession);
    }

    public StickySession getStickySessionById(String paramName, String paramValue) {
        return stickySessionCache.peek(routeUtils.getStickySessionId(paramName, paramValue));
    }

    public void deleteStickySession(StickySession stickySession) {
        stickySessionCache.remove(stickySession.getId());
    }
}