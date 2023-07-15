package io.surisoft.capi.lb.configuration;

import com.hazelcast.map.IMap;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.StickySession;
import org.cache2k.Cache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class CapiApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(CapiApplicationListener.class);

    @Autowired
    private Cache<String, Api> apiCache;

    @Autowired
    private IMap<String, StickySession> stickySessionCache;

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ContextClosedEvent) {
            log.info("Capi is shutting down, time to clear all cache info.");
            apiCache.clear();
            stickySessionCache.clear();
        }
    }
}