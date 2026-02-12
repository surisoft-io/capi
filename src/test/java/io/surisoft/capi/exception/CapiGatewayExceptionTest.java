package io.surisoft.capi.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CapiGatewayExceptionTest {

    @Test
    void testMessageIsSetViaConstructor() {
        String message = "Something went wrong";
        CapiGatewayException exceptionUnderTest = new CapiGatewayException(message);
        assertThat(exceptionUnderTest.getMessage()).isEqualTo(message);
    }

    @Test
    void testExtendsException() {
        CapiGatewayException exceptionUnderTest = new CapiGatewayException("test");
        assertThat(exceptionUnderTest).isInstanceOf(Exception.class);
    }
}
