package io.surisoft.capi.schema;

import org.eclipse.jetty.server.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GrpcClientTest {

    @Mock
    private Set<Mapping> mockMappingList;
    @Mock
    private Handler mockHandler;

    private GrpcClient grpcClientUnderTest;

    @BeforeEach
    void setUp() {
        grpcClientUnderTest = new GrpcClient();
        grpcClientUnderTest.setMappingList(mockMappingList);
        grpcClientUnderTest.setHandler(mockHandler);
    }

    @Test
    void testServiceIdGetterAndSetter() {
        final String serviceId = "serviceId";
        grpcClientUnderTest.setServiceId(serviceId);
        assertThat(grpcClientUnderTest.getServiceId()).isEqualTo(serviceId);
    }

    @Test
    void testPathGetterAndSetter() {
        final String path = "path";
        grpcClientUnderTest.setPath(path);
        assertThat(grpcClientUnderTest.getPath()).isEqualTo(path);
    }

    @Test
    void testGetHandler() {
        assertThat(grpcClientUnderTest.getHandler()).isEqualTo(mockHandler);
    }

    @Test
    void testGetMappingList() {
        assertThat(grpcClientUnderTest.getMappingList()).isEqualTo(mockMappingList);
    }

    @Test
    void testRequiresSubscriptionGetterAndSetter() {
        grpcClientUnderTest.setRequiresSubscription(true);
        assertThat(grpcClientUnderTest.isRequiresSubscription()).isTrue();
        assertThat(grpcClientUnderTest.requiresSubscription()).isTrue();
    }

    @Test
    void testSubscriptionRoleGetterAndSetter() {
        final String subscriptionRole = "subscriptionRole";
        grpcClientUnderTest.setSubscriptionRole(subscriptionRole);
        assertThat(grpcClientUnderTest.getSubscriptionRole()).isEqualTo(subscriptionRole);
    }

    @Test
    void testRootContextGetterAndSetter() {
        final String rootContext = "/root";
        grpcClientUnderTest.setRootContext(rootContext);
        assertThat(grpcClientUnderTest.getRootContext()).isEqualTo(rootContext);
    }
}
