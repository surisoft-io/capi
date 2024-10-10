package io.surisoft.capi.configuration;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.undertow.SSEGateway;
import io.surisoft.capi.undertow.WebsocketGateway;
import org.cache2k.Cache;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CapiApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(CapiApplicationListener.class);
    private final Cache<String, Service> serviceCache;
    private final Cache<String, StickySession> stickySessionCache;
    private final Optional<WebsocketGateway> websocketGateway;
    private final Optional<SSEGateway> sseGateway;

    public CapiApplicationListener(Cache<String, Service> serviceCache, Cache<String, StickySession> stickySessionCache, Optional<WebsocketGateway> websocketGateway, Optional<SSEGateway> sseGateway) {
        this.serviceCache = serviceCache;
        this.stickySessionCache = stickySessionCache;
        this.websocketGateway = websocketGateway;
        this.sseGateway = sseGateway;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ApplicationStartedEvent) {
            if(websocketGateway.isPresent()) {
                log.info("Capi Websocket Gateway starting.");
                websocketGateway.get().runProxy();
            }
            if(sseGateway.isPresent()) {
                log.info("Capi SSE Gateway starting.");
                sseGateway.get().runProxy();
            }
        }
        if(applicationEvent instanceof ContextClosedEvent) {
            log.info("Capi is shutting down, time to clear all cache info.");
            serviceCache.clear();
            stickySessionCache.clear();
        }
    }
}