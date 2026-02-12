package io.surisoft.capi.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessLogFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    private AccessLogFilter filterUnderTest;

    @BeforeEach
    void setUp() {
        filterUnderTest = new AccessLogFilter();
    }

    @Test
    void delegatesToFilterChain() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(200);

        filterUnderTest.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void usesRemoteAddrWhenNoXForwardedFor() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(200);

        filterUnderTest.doFilter(request, response, chain);

        verify(request).getRemoteAddr();
        verify(chain).doFilter(request, response);
    }

    @Test
    void usesXForwardedForWhenPresent() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/data");
        when(response.getStatus()).thenReturn(201);

        filterUnderTest.doFilter(request, response, chain);

        verify(request, atLeast(1)).getHeader("X-Forwarded-For");
        verify(chain).doFilter(request, response);
    }
}
