package io.surisoft.capi.gateway;

import io.surisoft.capi.tracer.CapiGatewayTracer;
import io.surisoft.capi.utils.Constants;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Context;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CAPIProxyHandlerTest {

    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private CapiGatewayTracer mockTracer;
    @Mock
    private Request mockRequest;

    @Test
    void testRewriteHttpUriRoundRobinCyclesThroughBackends() {
        URI backend1 = URI.create("http://host1:8080");
        URI backend2 = URI.create("http://host2:8081");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend1, backend2), null);

        when(mockRequest.getAttribute(Constants.FORWARDED_PATH_ATTR)).thenReturn("/path");
        when(mockRequest.getAttribute(Constants.SANITIZED_QUERY_ATTR)).thenReturn(null);
        HttpURI mockHttpUri = HttpURI.build().query("q=1").asImmutable();
        when(mockRequest.getHttpURI()).thenReturn(mockHttpUri);

        HttpURI result1 = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result1.getHost()).isEqualTo("host1");

        HttpURI result2 = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result2.getHost()).isEqualTo("host2");

        HttpURI result3 = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result3.getHost()).isEqualTo("host1");
    }

    @Test
    void testRewriteHttpUriReturnsNullWhenBackendsEmpty() {
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(), null);

        HttpURI result = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result).isNull();
    }

    @Test
    void testRewriteHttpUriUsesForwardedPathAttribute() {
        URI backend = URI.create("http://host1:8080");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend), null);

        when(mockRequest.getAttribute(Constants.FORWARDED_PATH_ATTR)).thenReturn("/forwarded/path");
        when(mockRequest.getAttribute(Constants.SANITIZED_QUERY_ATTR)).thenReturn(null);
        HttpURI mockHttpUri = HttpURI.build().query(null).asImmutable();
        when(mockRequest.getHttpURI()).thenReturn(mockHttpUri);

        HttpURI result = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result.getPath()).isEqualTo("/forwarded/path");
    }

    @Test
    void testRewriteHttpUriUsesSanitizedQueryAttribute() {
        URI backend = URI.create("http://host1:8080");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend), null);

        when(mockRequest.getAttribute(Constants.FORWARDED_PATH_ATTR)).thenReturn("/path");
        when(mockRequest.getAttribute(Constants.SANITIZED_QUERY_ATTR)).thenReturn("sanitized=true");

        HttpURI result = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result.getQuery()).isEqualTo("sanitized=true");
    }

    @Test
    void testRewriteHttpUriFallsBackToRequestPathAndQuery() {
        URI backend = URI.create("http://host1:8080");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend), null);

        when(mockRequest.getAttribute(Constants.FORWARDED_PATH_ATTR)).thenReturn(null);
        when(mockRequest.getAttribute(Constants.SANITIZED_QUERY_ATTR)).thenReturn(null);
        // When forwarded path is null, Request.getPathInContext(request) is called.
        // That static method calls request.getContext().getPathInContext(canonicalPath).
        HttpURI mockHttpUri = HttpURI.build().path("/fallback/path").query("key=value").asImmutable();
        when(mockRequest.getHttpURI()).thenReturn(mockHttpUri);
        Context mockContext = mock(Context.class);
        when(mockContext.getPathInContext("/fallback/path")).thenReturn("/fallback/path");
        when(mockRequest.getContext()).thenReturn(mockContext);

        HttpURI result = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result.getPath()).isEqualTo("/fallback/path");
        assertThat(result.getQuery()).isEqualTo("key=value");
    }

    @Test
    void testRewriteHttpUriCallsTracerWhenPresent() {
        URI backend = URI.create("http://host1:8080");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend), mockTracer);

        when(mockRequest.getAttribute(Constants.FORWARDED_PATH_ATTR)).thenReturn("/path");
        when(mockRequest.getAttribute(Constants.SANITIZED_QUERY_ATTR)).thenReturn(null);
        HttpURI mockHttpUri = HttpURI.build().asImmutable();
        when(mockRequest.getHttpURI()).thenReturn(mockHttpUri);

        handlerUnderTest.rewriteHttpURI(mockRequest);
        verify(mockTracer).capiProxyRequest(backend);
    }

    @Test
    void testRewriteHttpUriWorksWithNullTracer() {
        URI backend = URI.create("http://host1:8080");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend), null);

        when(mockRequest.getAttribute(Constants.FORWARDED_PATH_ATTR)).thenReturn("/path");
        when(mockRequest.getAttribute(Constants.SANITIZED_QUERY_ATTR)).thenReturn(null);
        HttpURI mockHttpUri = HttpURI.build().asImmutable();
        when(mockRequest.getHttpURI()).thenReturn(mockHttpUri);

        HttpURI result = handlerUnderTest.rewriteHttpURI(mockRequest);
        assertThat(result).isNotNull();
        assertThat(result.getScheme()).isEqualTo("http");
        assertThat(result.getHost()).isEqualTo("host1");
        assertThat(result.getPort()).isEqualTo(8080);
    }

    @Test
    void testToStringNoBackends() {
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(), null);
        assertThat(handlerUnderTest.toString()).isEqualTo("CAPIProxyHandler - no backends");
    }

    @Test
    void testToStringSingleBackend() {
        URI backend = URI.create("http://host1:8080");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend), null);
        assertThat(handlerUnderTest.toString()).isEqualTo("reverse-proxy( 'http://host1:8080' )");
    }

    @Test
    void testToStringMultipleBackends() {
        URI backend1 = URI.create("http://host1:8080");
        URI backend2 = URI.create("http://host2:8081");
        CAPIProxyHandler handlerUnderTest = new CAPIProxyHandler(mockHttpClient, List.of(backend1, backend2), null);
        assertThat(handlerUnderTest.toString()).isEqualTo("reverse-proxy( { 'http://host1:8080', 'http://host2:8081' } )");
    }
}
