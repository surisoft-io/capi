package io.surisoft.capi.configuration;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CamelStartupListenerTest {

    @Mock
    private CamelContext camelContext;

    @Test
    void consulEnabled_addRoutesCalled() throws Exception {
        CamelStartupListener listenerUnderTest = new CamelStartupListener(5000, true);

        listenerUnderTest.onCamelContextFullyStarted(camelContext, false);

        verify(camelContext).addRoutes(any(RouteBuilder.class));
    }

    @Test
    void consulDisabled_addRoutesNeverCalled() throws Exception {
        CamelStartupListener listenerUnderTest = new CamelStartupListener(5000, false);

        listenerUnderTest.onCamelContextFullyStarted(camelContext, false);

        verify(camelContext, never()).addRoutes(any());
    }
}
