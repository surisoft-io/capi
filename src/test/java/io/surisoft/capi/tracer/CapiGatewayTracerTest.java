package io.surisoft.capi.tracer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapiGatewayTracerTest {

    @Mock
    private HttpUtils mockHttpUtils;
    @Mock
    private Tracer mockTracer;
    @Mock
    private Span mockSpan;

    private CapiGatewayTracer tracerUnderTest;

    @BeforeEach
    void setUp() {
        tracerUnderTest = new CapiGatewayTracer(mockHttpUtils, mockTracer);
    }

    @Test
    void testCapiProxyRequestSetsAttributesAndEndsSpan() throws Exception {
        pushSpanViaReflection(mockSpan);

        URI host = URI.create("https://backend.example.com:8443/api/resource?key=value");
        tracerUnderTest.capiProxyRequest(host);

        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_HOST, "backend.example.com");
        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_PORT, "8443");
        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_PATH, "/api/resource");
        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_QUERY, "key=value");
        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_SCHEME, "https");
        verify(mockSpan).end();
    }

    @Test
    void testCapiProxyRequestHandlesNullPopGracefully() {
        // No span pushed, so pop returns null
        assertThatCode(() -> tracerUnderTest.capiProxyRequest(URI.create("http://host:80")))
                .doesNotThrowAnyException();
        verifyNoInteractions(mockSpan);
    }

    @Test
    void testCapiProxyRequestSkipsPathWhenEmpty() throws Exception {
        pushSpanViaReflection(mockSpan);

        // URI with no path or query (opaque-like)
        URI host = URI.create("http://host:80");
        tracerUnderTest.capiProxyRequest(host);

        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_HOST, "host");
        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_PORT, "80");
        verify(mockSpan).setAttribute(Constants.CAPI_WS_CLIENT_SCHEME, "http");
        verify(mockSpan, never()).setAttribute(eq(Constants.CAPI_WS_CLIENT_PATH), anyString());
        verify(mockSpan, never()).setAttribute(eq(Constants.CAPI_WS_CLIENT_QUERY), anyString());
        verify(mockSpan).end();
    }

    private void pushSpanViaReflection(Span span) throws Exception {
        Field tracingStateField = CapiGatewayTracer.class.getDeclaredField("tracingState");
        tracingStateField.setAccessible(true);
        CapiTracingState state = (CapiTracingState) tracingStateField.get(tracerUnderTest);
        state.pushServerSpan(span);
    }
}
