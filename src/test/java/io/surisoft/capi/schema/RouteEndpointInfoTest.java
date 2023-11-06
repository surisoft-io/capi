package io.surisoft.capi.schema;

import org.apache.camel.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RouteEndpointInfoTest {

    @Mock
    private Route mockRoute;
    @Mock
    private Map<String, Object> mockProperties;

    private RouteEndpointInfo routeEndpointInfoUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        routeEndpointInfoUnderTest = new RouteEndpointInfo(mockRoute);
        ReflectionTestUtils.setField(routeEndpointInfoUnderTest, "properties", mockProperties);
    }

    @Test
    void testIdGetterAndSetter() {
        final String id = "id";
        routeEndpointInfoUnderTest.setId(id);
        assertThat(routeEndpointInfoUnderTest.getId()).isEqualTo(id);
    }

    @Test
    void testGetGroup() {
        assertThat(routeEndpointInfoUnderTest.getGroup()).isEqualTo("group");
    }

    @Test
    void testGetProperties() {
        assertThat(routeEndpointInfoUnderTest.getProperties()).isEqualTo(mockProperties);
    }

    @Test
    void testGetDescription() {
        assertThat(routeEndpointInfoUnderTest.getDescription()).isEqualTo("description");
    }

    @Test
    void testGetUptime() {
        assertThat(routeEndpointInfoUnderTest.getUptime()).isEqualTo("uptime");
    }

    @Test
    void testGetUptimeMillis() {
        assertThat(routeEndpointInfoUnderTest.getUptimeMillis()).isEqualTo(0L);
    }

    @Test
    void testGetStatus() {
        assertThat(routeEndpointInfoUnderTest.getStatus()).isEqualTo("status");
    }
}
