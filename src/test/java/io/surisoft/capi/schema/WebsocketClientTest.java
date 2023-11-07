package io.surisoft.capi.schema;

import io.undertow.server.HttpHandler;
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
    private HttpHandler mockHttpHandler;

    private WebsocketClient websocketClientUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        websocketClientUnderTest = new WebsocketClient();
        websocketClientUnderTest.setMappingList(mockMappingList);
        websocketClientUnderTest.setHttpHandler(mockHttpHandler);
    }

    @Test
    void testPathGetterAndSetter() {
        final String path = "path";
        websocketClientUnderTest.setPath(path);
        assertThat(websocketClientUnderTest.getPath()).isEqualTo(path);
    }

    @Test
    void testGetHttpHandler() {
        assertThat(websocketClientUnderTest.getHttpHandler()).isEqualTo(mockHttpHandler);
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
        websocketClientUnderTest.setApiId(apiId);
        assertThat(websocketClientUnderTest.getApiId()).isEqualTo(apiId);
    }

    @Test
    void testGetMappingList() {
        assertThat(websocketClientUnderTest.getMappingList()).isEqualTo(mockMappingList);
    }
}
