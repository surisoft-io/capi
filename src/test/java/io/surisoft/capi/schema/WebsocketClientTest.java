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
class WebsocketClientTest {

    @Mock
    private Set<Mapping> mockMappingList;
    @Mock
    private Handler mockHandler;

    private WebsocketClient websocketClientUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        websocketClientUnderTest = new WebsocketClient();
        websocketClientUnderTest.setMappingList(mockMappingList);
        websocketClientUnderTest.setHandler(mockHandler);
    }

    @Test
    void testPathGetterAndSetter() {
        final String path = "path";
        websocketClientUnderTest.setPath(path);
        assertThat(websocketClientUnderTest.getPath()).isEqualTo(path);
    }

    @Test
    void testGetHandler() {
        assertThat(websocketClientUnderTest.getHandler()).isEqualTo(mockHandler);
    }

    @Test
    void testRequiresSubscriptionGetterAndSetter() {
        final boolean requiresSubscription = false;
        websocketClientUnderTest.setRequiresSubscription(requiresSubscription);
        assertThat(websocketClientUnderTest.requiresSubscription()).isFalse();
    }

    @Test
    void testSubscriptionRoleGetterAndSetter() {
        final String subscriptionRole = "subscriptionRole";
        websocketClientUnderTest.setSubscriptionRole(subscriptionRole);
        assertThat(websocketClientUnderTest.getSubscriptionRole()).isEqualTo(subscriptionRole);
    }

    @Test
    void testApiIdGetterAndSetter() {
        final String apiId = "apiId";
        websocketClientUnderTest.setServiceId(apiId);
        assertThat(websocketClientUnderTest.getServiceId()).isEqualTo(apiId);
    }

    @Test
    void testGetMappingList() {
        assertThat(websocketClientUnderTest.getMappingList()).isEqualTo(mockMappingList);
    }
}
