package io.surisoft.capi.configuration;

import io.surisoft.capi.gateway.GrpcGateway;
import io.surisoft.capi.gateway.SSEGateway;
import io.surisoft.capi.gateway.WebsocketGateway;
import io.surisoft.capi.schema.Service;
import org.cache2k.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapiApplicationListenerTest {

    @Mock
    private Cache<String, Service> serviceCache;
    @Mock
    private WebsocketGateway websocketGateway;
    @Mock
    private SSEGateway sseGateway;
    @Mock
    private GrpcGateway grpcGateway;
    @Mock
    private ApplicationStartedEvent applicationStartedEvent;
    @Mock
    private ContextClosedEvent contextClosedEvent;
    @Mock
    private ContextRefreshedEvent contextRefreshedEvent;

    @Test
    void applicationStartedEvent_callsRunProxyOnAllGateways() {
        CapiApplicationListener listenerUnderTest = new CapiApplicationListener(
                serviceCache,
                Optional.of(websocketGateway),
                Optional.of(sseGateway),
                Optional.of(grpcGateway)
        );

        listenerUnderTest.onApplicationEvent(applicationStartedEvent);

        verify(websocketGateway).runProxy();
        verify(sseGateway).runProxy();
        verify(grpcGateway).runProxy();
    }

    @Test
    void applicationStartedEventWithEmptyOptionals_noCalls() {
        CapiApplicationListener listenerUnderTest = new CapiApplicationListener(
                serviceCache,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        listenerUnderTest.onApplicationEvent(applicationStartedEvent);

        verifyNoInteractions(serviceCache);
    }

    @Test
    void contextClosedEvent_clearsCacheAndStopsGateways() {
        CapiApplicationListener listenerUnderTest = new CapiApplicationListener(
                serviceCache,
                Optional.of(websocketGateway),
                Optional.of(sseGateway),
                Optional.of(grpcGateway)
        );

        listenerUnderTest.onApplicationEvent(contextClosedEvent);

        verify(serviceCache).clear();
        verify(websocketGateway).stop();
        verify(sseGateway).stop();
        verify(grpcGateway).stop();
    }

    @Test
    void unrelatedEventType_noAction() {
        CapiApplicationListener listenerUnderTest = new CapiApplicationListener(
                serviceCache,
                Optional.of(websocketGateway),
                Optional.of(sseGateway),
                Optional.of(grpcGateway)
        );

        listenerUnderTest.onApplicationEvent(contextRefreshedEvent);

        verifyNoInteractions(serviceCache, websocketGateway, sseGateway, grpcGateway);
    }
}
