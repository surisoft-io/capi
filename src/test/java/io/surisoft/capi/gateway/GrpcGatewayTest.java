package io.surisoft.capi.gateway;

import io.surisoft.capi.schema.GrpcClient;
import io.surisoft.capi.utils.WebsocketUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class GrpcGatewayTest {

    @Mock
    private WebsocketUtils mockWebsocketUtils;

    private GrpcGateway gatewayUnderTest;

    @BeforeEach
    void setUp() {
        gatewayUnderTest = new GrpcGateway(
                50051,
                Optional.empty(),
                mockWebsocketUtils,
                new HashMap<>()
        );
    }

    @Test
    void testStopWorksWhenServerIsNull() {
        // server is null because runProxy() was never called
        assertThatCode(() -> gatewayUnderTest.stop()).doesNotThrowAnyException();
    }

    @Test
    void testConstructorStoresFieldsCorrectly() throws Exception {
        Field grpcPortField = GrpcGateway.class.getDeclaredField("grpcPort");
        grpcPortField.setAccessible(true);
        assertThat(grpcPortField.getInt(gatewayUnderTest)).isEqualTo(50051);

        Field sslContextField = GrpcGateway.class.getDeclaredField("sslContext");
        sslContextField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Optional<?> sslContext = (Optional<?>) sslContextField.get(gatewayUnderTest);
        assertThat(sslContext).isEmpty();

        Field websocketUtilsField = GrpcGateway.class.getDeclaredField("websocketUtils");
        websocketUtilsField.setAccessible(true);
        assertThat(websocketUtilsField.get(gatewayUnderTest)).isSameAs(mockWebsocketUtils);

        Field grpcClientsField = GrpcGateway.class.getDeclaredField("grpcClients");
        grpcClientsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, GrpcClient> grpcClients = (Map<String, GrpcClient>) grpcClientsField.get(gatewayUnderTest);
        assertThat(grpcClients).isEmpty();
    }
}
