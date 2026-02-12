package io.surisoft.capi.tracer;

import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CapiTracingStateTest {

    @Mock
    private Span mockSpan1;
    @Mock
    private Span mockSpan2;

    private CapiTracingState tracingStateUnderTest;

    @BeforeEach
    void setUp() {
        tracingStateUnderTest = new CapiTracingState();
    }

    @Test
    void testPushAndPopSpanLifoOrdering() {
        tracingStateUnderTest.pushServerSpan(mockSpan1);
        tracingStateUnderTest.pushServerSpan(mockSpan2);

        assertThat(tracingStateUnderTest.popServerSpan()).isSameAs(mockSpan2);
        assertThat(tracingStateUnderTest.popServerSpan()).isSameAs(mockSpan1);
    }

    @Test
    void testPopFromEmptyReturnsNull() {
        assertThat(tracingStateUnderTest.popServerSpan()).isNull();
    }

    @Test
    void testSafeCopyCreatesIndependentCopyWithSameSpans() {
        tracingStateUnderTest.pushServerSpan(mockSpan1);
        tracingStateUnderTest.pushServerSpan(mockSpan2);

        CapiTracingState copy = (CapiTracingState) tracingStateUnderTest.safeCopy();

        // Copy should have the same spans
        assertThat(copy.popServerSpan()).isSameAs(mockSpan2);
        assertThat(copy.popServerSpan()).isSameAs(mockSpan1);

        // Original should still have its spans (independent copy)
        assertThat(tracingStateUnderTest.popServerSpan()).isSameAs(mockSpan2);
        assertThat(tracingStateUnderTest.popServerSpan()).isSameAs(mockSpan1);
    }

    @Test
    void testKeyConstant() {
        assertThat(CapiTracingState.KEY).isEqualTo("CapiTracingState");
    }
}
