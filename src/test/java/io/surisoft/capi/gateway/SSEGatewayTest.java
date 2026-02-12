package io.surisoft.capi.gateway;

import io.surisoft.capi.utils.SSEUtils;
import io.surisoft.capi.schema.SSEClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class SSEGatewayTest {

    @Mock
    private SSEUtils mockSSEUtils;

    private SSEGateway gatewayUnderTest;

    @BeforeEach
    void setUp() {
        gatewayUnderTest = new SSEGateway(
                8081,
                new HashMap<>(),
                mockSSEUtils,
                Optional.empty(),
                List.of("Authorization", "Content-Type"),
                "oauth2_cookie"
        );
    }

    @Test
    void testIsValidOriginWithValidUrl() {
        assertThat(gatewayUnderTest.isValidOrigin("http://example.com")).isTrue();
        assertThat(gatewayUnderTest.isValidOrigin("https://example.com:8443/path")).isTrue();
    }

    @Test
    void testIsValidOriginWithInvalidUrl() {
        assertThat(gatewayUnderTest.isValidOrigin("not-a-url")).isFalse();
        assertThat(gatewayUnderTest.isValidOrigin("")).isFalse();
    }

    @Test
    void testStopWorksWhenServerIsNull() {
        // server is null because runProxy() was never called
        assertThatCode(() -> gatewayUnderTest.stop()).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testConstructorInitializesManagedHeaders() throws Exception {
        Field managedHeadersField = SSEGateway.class.getDeclaredField("managedHeaders");
        managedHeadersField.setAccessible(true);
        Map<String, String> managedHeaders = (Map<String, String>) managedHeadersField.get(gatewayUnderTest);

        assertThat(managedHeaders).containsKey("Access-Control-Allow-Credentials");
        assertThat(managedHeaders).containsKey("Access-Control-Allow-Methods");
        assertThat(managedHeaders.get("Access-Control-Allow-Headers")).contains("Authorization");
        assertThat(managedHeaders.get("Access-Control-Allow-Headers")).contains("Content-Type");
    }
}
