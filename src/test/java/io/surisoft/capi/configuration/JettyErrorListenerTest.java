package io.surisoft.capi.configuration;

import io.surisoft.capi.schema.CapiRestError;
import io.surisoft.capi.utils.Constants;
import org.eclipse.jetty.http.HttpFields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JettyErrorListenerTest {

    @Test
    void testBuildCapiErrorObjectWithAllHeaders() {
        // Use mock with CALLS_REAL_METHODS to avoid calling the constructor (which starts a server)
        JettyErrorListener listenerUnderTest = mock(JettyErrorListener.class, CALLS_REAL_METHODS);

        HttpFields headers = HttpFields.build()
                .put(Constants.REASON_CODE_HEADER, "500")
                .put(Constants.REASON_MESSAGE_HEADER, "Internal Server Error")
                .put(Constants.ROUTE_ID_HEADER, "route-123")
                .put(Constants.CAPI_URI_IN_ERROR, "http://backend:8080/api")
                .put(Constants.TRACE_ID_HEADER, "trace-abc-123")
                .asImmutable();

        CapiRestError result = listenerUnderTest.buildCapiErrorObject(headers);

        assertThat(result.getErrorCode()).isEqualTo(500);
        assertThat(result.getErrorMessage()).isEqualTo("Internal Server Error");
        assertThat(result.getRouteID()).isEqualTo("route-123");
        assertThat(result.getHttpUri()).isEqualTo("http://backend:8080/api");
        assertThat(result.getTraceID()).isEqualTo("trace-abc-123");
    }

    @Test
    void testBuildCapiErrorObjectWithNoHeaders() {
        JettyErrorListener listenerUnderTest = mock(JettyErrorListener.class, CALLS_REAL_METHODS);

        HttpFields headers = HttpFields.build().asImmutable();

        CapiRestError result = listenerUnderTest.buildCapiErrorObject(headers);

        assertThat(result.getErrorCode()).isZero();
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getRouteID()).isNull();
        assertThat(result.getHttpUri()).isNull();
        assertThat(result.getTraceID()).isNull();
    }

    @Test
    void testBuildCapiErrorObjectWithPartialHeaders() {
        JettyErrorListener listenerUnderTest = mock(JettyErrorListener.class, CALLS_REAL_METHODS);

        HttpFields headers = HttpFields.build()
                .put(Constants.REASON_CODE_HEADER, "404")
                .put(Constants.REASON_MESSAGE_HEADER, "Not Found")
                .asImmutable();

        CapiRestError result = listenerUnderTest.buildCapiErrorObject(headers);

        assertThat(result.getErrorCode()).isEqualTo(404);
        assertThat(result.getErrorMessage()).isEqualTo("Not Found");
        assertThat(result.getRouteID()).isNull();
        assertThat(result.getHttpUri()).isNull();
        assertThat(result.getTraceID()).isNull();
    }
}
