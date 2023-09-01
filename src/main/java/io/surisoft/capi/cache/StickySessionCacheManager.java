package io.surisoft.capi.cache;

import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.utils.RouteUtils;
import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StickySessionCacheManager {

    @Autowired
    private RouteUtils routeUtils;

    @Autowired
    private Cache<String, StickySession> stickySessionCache;

    public void createStickySession(StickySession stickySession) {
        stickySessionCache.put(stickySession.getId(), stickySession);
    }

    public StickySession getStickySessionById(String paramName, String paramValue) {
        String stickySessionId = routeUtils.getStickySessionId(paramName, paramValue);
        if(stickySessionCache.containsKey(stickySessionId)) {
            return stickySessionCache.get(stickySessionId);
        }
        return null;
    }

    public void deleteStickySession(StickySession stickySession) {
        stickySessionCache.remove(stickySession.getId());
    }
}