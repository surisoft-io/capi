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
class SSEClientTest {

    @Mock
    private Set<Mapping> mockMappingList;
    @Mock
    private Handler mockHandler;

    private SSEClient sseClientUnderTest;

    @BeforeEach
    void setUp() {
        sseClientUnderTest = new SSEClient();
        sseClientUnderTest.setMappingList(mockMappingList);
        sseClientUnderTest.setHandler(mockHandler);
    }

    @Test
    void testPathGetterAndSetter() {
        final String path = "path";
        sseClientUnderTest.setPath(path);
        assertThat(sseClientUnderTest.getPath()).isEqualTo(path);
    }

    @Test
    void testGetHandler() {
        assertThat(sseClientUnderTest.getHandler()).isEqualTo(mockHandler);
    }

    @Test
    void testApiIdGetterAndSetter() {
        final String apiId = "apiId";
        sseClientUnderTest.setApiId(apiId);
        assertThat(sseClientUnderTest.getApiId()).isEqualTo(apiId);
    }

    @Test
    void testGetMappingList() {
        assertThat(sseClientUnderTest.getMappingList()).isEqualTo(mockMappingList);
    }

    @Test
    void testRequiresSubscriptionGetterAndSetter() {
        sseClientUnderTest.setRequiresSubscription(true);
        assertThat(sseClientUnderTest.isRequiresSubscription()).isTrue();
        assertThat(sseClientUnderTest.requiresSubscription()).isTrue();
    }

    @Test
    void testSubscriptionRoleGetterAndSetter() {
        final String subscriptionRole = "subscriptionRole";
        sseClientUnderTest.setSubscriptionRole(subscriptionRole);
        assertThat(sseClientUnderTest.getSubscriptionRole()).isEqualTo(subscriptionRole);
    }
}
