package io.surisoft.capi.service;

import org.apache.camel.impl.event.ExchangeFailedEvent;
import org.apache.camel.impl.event.RouteRemovedEvent;
import org.apache.camel.impl.event.RouteStoppedEvent;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

//@Component
public class CapiEventNotifier extends EventNotifierSupport {
    @Autowired
    private Cache<String, String> startRouteStoppedEventCache;
    @Autowired
    private Cache<String, String> startRouteRemovedEventCache;
    @Autowired
    private Cache<String, String> startExchangeFailedEventCache;

    @Override
    public void notify(CamelEvent event) {
        if (event instanceof RouteStoppedEvent) {
            startRouteStoppedEventCache.put(UUID.randomUUID().toString(), ((RouteStoppedEvent) event).getRoute().getRouteId());
        }
        if(event instanceof RouteRemovedEvent) {
            startRouteRemovedEventCache.put(UUID.randomUUID().toString(), ((RouteRemovedEvent) event).getRoute().getRouteId());
        }
        if(event instanceof ExchangeFailedEvent exchangeFailedEvent) {
            startExchangeFailedEventCache.put(exchangeFailedEvent.getExchange().getExchangeId(), exchangeFailedEvent.getCause().getMessage());
        }
    }
}