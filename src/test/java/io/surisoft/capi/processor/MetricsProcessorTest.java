package io.surisoft.capi.processor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.RequiredSearch;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsProcessorTest {

    @Mock
    private CompositeMeterRegistry meterRegistry;
    @Mock
    private Exchange exchange;
    @Mock
    private RequiredSearch requiredSearch;
    @Mock
    private Counter counter;

    @InjectMocks
    private MetricsProcessor processorUnderTest;

    @Test
    void routeIdPresent_counterIncremented() {
        when(exchange.getFromRouteId()).thenReturn("my-route");
        when(meterRegistry.get("my-route")).thenReturn(requiredSearch);
        when(requiredSearch.counter()).thenReturn(counter);

        processorUnderTest.process(exchange);

        verify(counter).increment();
    }

    @Test
    void routeIdNull_noCounterInteraction() {
        when(exchange.getFromRouteId()).thenReturn(null);

        processorUnderTest.process(exchange);

        verifyNoInteractions(meterRegistry);
    }
}
