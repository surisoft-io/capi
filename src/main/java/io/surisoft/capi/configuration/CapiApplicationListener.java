package io.surisoft.capi.configuration;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.websocket.WebsocketGateway;
import org.cache2k.Cache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class CapiApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(CapiApplicationListener.class);

    @Autowired
    private Cache<String, Service> serviceCache;

    @Autowired
    private Cache<String, StickySession> stickySessionCache;

    @Autowired(required = false)
    private WebsocketGateway websocketGateway;

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ApplicationStartedEvent) {
            if(websocketGateway != null) {
                log.info("Capi Websocket Gateway starting.");
                websocketGateway.runProxy();
            }
        }
        if(applicationEvent instanceof ContextClosedEvent) {
            log.info("Capi is shutting down, time to clear all cache info.");
            serviceCache.clear();
            stickySessionCache.clear();
        }
    }
}