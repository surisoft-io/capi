package io.surisoft.capi.schema;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RouteDetailsEndpointInfoTest {

    @Mock
    private CamelContext mockCamelContext;
    @Mock
    private Route mockRoute;
    @Mock
    private RouteDetails mockRouteDetails;

    private RouteDetailsEndpointInfo routeDetailsEndpointInfoUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        routeDetailsEndpointInfoUnderTest = new RouteDetailsEndpointInfo(mockCamelContext, mockRoute);
        ReflectionTestUtils.setField(routeDetailsEndpointInfoUnderTest, "routeDetails", mockRouteDetails);
    }
}
