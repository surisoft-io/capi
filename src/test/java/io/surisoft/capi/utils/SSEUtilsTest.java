package io.surisoft.capi.utils;

import io.surisoft.capi.exception.CapiGatewayException;
import io.surisoft.capi.schema.SSEClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SSEUtilsTest {

    private SSEUtils sseUtilsUnderTest;

    @BeforeEach
    void setUp() {
        sseUtilsUnderTest = new SSEUtils("/capi/*", Optional.empty());
    }

    @Test
    void testNormalizePathForForwardingStripsContextPathAndApiId() {
        SSEClient sseClient = new SSEClient();
        sseClient.setApiId("myapi");

        String result = sseUtilsUnderTest.normalizePathForForwarding(sseClient, "/capi/myapi/v1/resource");
        assertThat(result).isEqualTo("//v1/resource");
    }

    @Test
    void testNormalizeBaseContextName() {
        String result = sseUtilsUnderTest.normalizeBaseContextName();
        assertThat(result).isEqualTo("capi");
    }

    @Test
    void testNormalizeCapiContextPath() {
        String result = sseUtilsUnderTest.normalizeCapiContextPath();
        assertThat(result).isEqualTo("/capi");
    }

    @Test
    void testGetPathDefinitionReturnsNullForPathsWithLessThanFourParts() {
        assertThat(sseUtilsUnderTest.getPathDefinition("/capi/myapi")).isNull();
    }

    @Test
    void testGetPathDefinitionReturnsNullWhenContextDoesNotMatch() {
        assertThat(sseUtilsUnderTest.getPathDefinition("/other/myapi/v1/resource")).isNull();
    }

    @Test
    void testGetPathDefinitionReturnsCorrectPathForValidInput() {
        String result = sseUtilsUnderTest.getPathDefinition("/capi/myapi/v1/resource");
        assertThat(result).isEqualTo("/capi/myapi/v1/");
    }

    @Test
    void testGetWebclientIdReturnsNullForPathsWithLessThanFourParts() {
        assertThat(sseUtilsUnderTest.getWebclientId("/capi/myapi")).isNull();
    }

    @Test
    void testGetWebclientIdReturnsCorrectId() {
        String result = sseUtilsUnderTest.getWebclientId("/capi/myapi/v1/resource");
        assertThat(result).isEqualTo("/myapi/v1");
    }

    @Test
    void testCreateSSEAuthorizationThrowsWhenNoJwtProcessor() {
        assertThatThrownBy(() -> sseUtilsUnderTest.createSSEAuthorization())
                .isInstanceOf(CapiGatewayException.class)
                .hasMessageContaining("No OIDC provider enabled");
    }
}
