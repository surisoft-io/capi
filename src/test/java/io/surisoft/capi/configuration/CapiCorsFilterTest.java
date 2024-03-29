package io.surisoft.capi.configuration;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ServiceMeta;
import jakarta.servlet.FilterChain;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class CapiCorsFilterTest {

    private final Cache<String, Service> mockServiceCache = new Cache2kBuilder<String, Service>(){}
                                                            .name("serviceCache-" + hashCode())
                                                            .eternal(true)
                                                            .entryCapacity(10000)
                                                            .storeByReference(true)
                                                            .build();

    @Autowired
    private CapiCorsFilter capiCorsFilterUnderTest;

    @BeforeEach
    void setUp() {
        capiCorsFilterUnderTest.corsFilterComponent();
        ReflectionTestUtils.setField(capiCorsFilterUnderTest, "oauth2CookieName", "oauth2CookieName");
        ReflectionTestUtils.setField(capiCorsFilterUnderTest, "gatewayCorsManagementEnabled", true);
        ReflectionTestUtils.setField(capiCorsFilterUnderTest, "capiContextPath", "/capi");
        ReflectionTestUtils.setField(capiCorsFilterUnderTest, "serviceCache", mockServiceCache);
    }

    @Test
    void testDoFilterWithCapiNoAllowHeadersPresent() throws Exception {
        // Setup
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setRequestURI("/capi/test/endpoint");
        mockHttpServletRequest.addHeader("Origin", "http://localhost:8080");

        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        Service service = new Service();
        service.setId("test:endpoint");
        service.setName("Test");
        service.setContext("/test/endpoint");
        ServiceMeta serviceMeta = new ServiceMeta();
        service.setServiceMeta(serviceMeta);
        mockServiceCache.put("test:endpoint", service);

        // Run the test
        capiCorsFilterUnderTest.doFilter(mockHttpServletRequest, servletResponse, filterChain);
        Assertions.assertEquals(servletResponse.getHeader("Access-Control-Allow-Origin"), "http://localhost:8080");
    }

    @Test
    void testDoFilterWithCapiAllowHeadersWithOriginPresent() throws Exception {
        // Setup
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setRequestURI("/capi/test/endpoint");
        mockHttpServletRequest.addHeader("Origin", "http://localhost:8080");

        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        Service service = new Service();
        service.setId("test:endpoint");
        service.setName("Test");
        service.setContext("/test/endpoint");
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setAllowedOrigins("http://localhost:8080");
        service.setServiceMeta(serviceMeta);
        mockServiceCache.put("test:endpoint", service);

        // Run the test
        capiCorsFilterUnderTest.doFilter(mockHttpServletRequest, servletResponse, filterChain);
        Assertions.assertEquals(servletResponse.getHeader("Access-Control-Allow-Origin"), "http://localhost:8080");
    }

    @Test
    void testDoFilterWithCapiAllowHeadersWithoutOriginPresent() throws Exception {
        // Setup
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setRequestURI("/capi/test/endpoint");
        mockHttpServletRequest.addHeader("Origin", "http://localhost:8080");

        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        final FilterChain filterChain = Mockito.mock(FilterChain.class);

        Service service = new Service();
        service.setId("test:endpoint");
        service.setName("Test");
        service.setContext("/test/endpoint");
        ServiceMeta serviceMeta = new ServiceMeta();
        serviceMeta.setAllowedOrigins("http://localhost:9090");
        service.setServiceMeta(serviceMeta);
        mockServiceCache.put("test:endpoint", service);

        // Run the test
        capiCorsFilterUnderTest.doFilter(mockHttpServletRequest, servletResponse, filterChain);
        Assertions.assertFalse(servletResponse.containsHeader("Access-Control-Allow-Origin"));
    }
}