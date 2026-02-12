package io.surisoft.capi.service;

import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.impl.event.ExchangeFailedEvent;
import org.apache.camel.impl.event.RouteRemovedEvent;
import org.apache.camel.impl.event.RouteStoppedEvent;
import org.apache.camel.spi.CamelEvent;
import org.cache2k.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapiEventNotifierTest {

    @Mock
    private Cache<String, String> startRouteStoppedEventCache;
    @Mock
    private Cache<String, String> startRouteRemovedEventCache;
    @Mock
    private Cache<String, String> startExchangeFailedEventCache;

    private CapiEventNotifier notifierUnderTest;

    @BeforeEach
    void setUp() {
        notifierUnderTest = new CapiEventNotifier();
        ReflectionTestUtils.setField(notifierUnderTest, "startRouteStoppedEventCache", startRouteStoppedEventCache);
        ReflectionTestUtils.setField(notifierUnderTest, "startRouteRemovedEventCache", startRouteRemovedEventCache);
        ReflectionTestUtils.setField(notifierUnderTest, "startExchangeFailedEventCache", startExchangeFailedEventCache);
    }

    @Test
    void routeStoppedEvent_storesRouteIdInStoppedCache() {
        Route route = mock(Route.class);
        when(route.getRouteId()).thenReturn("my-route-1");
        RouteStoppedEvent event = new RouteStoppedEvent(route);

        notifierUnderTest.notify(event);

        verify(startRouteStoppedEventCache).put(anyString(), eq("my-route-1"));
        verifyNoInteractions(startRouteRemovedEventCache, startExchangeFailedEventCache);
    }

    @Test
    void routeRemovedEvent_storesRouteIdInRemovedCache() {
        Route route = mock(Route.class);
        when(route.getRouteId()).thenReturn("my-route-2");
        RouteRemovedEvent event = new RouteRemovedEvent(route);

        notifierUnderTest.notify(event);

        verify(startRouteRemovedEventCache).put(anyString(), eq("my-route-2"));
        verifyNoInteractions(startRouteStoppedEventCache, startExchangeFailedEventCache);
    }

    @Test
    void exchangeFailedEvent_storesCauseMessageKeyedByExchangeId() {
        Exchange exchange = mock(Exchange.class);
        when(exchange.getExchangeId()).thenReturn("exchange-123");
        when(exchange.getException()).thenReturn(new RuntimeException("Connection refused"));

        ExchangeFailedEvent event = new ExchangeFailedEvent(exchange);

        notifierUnderTest.notify(event);

        verify(startExchangeFailedEventCache).put("exchange-123", "Connection refused");
        verifyNoInteractions(startRouteStoppedEventCache, startRouteRemovedEventCache);
    }
}
