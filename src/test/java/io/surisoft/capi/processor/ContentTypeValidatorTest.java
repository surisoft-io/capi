package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentTypeValidatorTest {

    @Mock
    private Exchange exchange;
    @Mock
    private Message message;

    private ContentTypeValidator validatorUnderTest;

    @BeforeEach
    void setUp() {
        validatorUnderTest = new ContentTypeValidator();
    }

    @Test
    void contentTypeEventStream_throws400Exception() throws Exception {
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Constants.CONTENT_TYPE)).thenReturn("text/event-stream");

        validatorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_CODE_HEADER, 400);
        verify(exchange).setException(any(AuthorizationException.class));
    }

    @Test
    void acceptEventStream_throws400Exception() throws Exception {
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Constants.CONTENT_TYPE)).thenReturn(null);
        when(message.getHeader("content-type")).thenReturn(null);
        when(message.getHeader(Constants.ACCEPT_TYPE)).thenReturn("text/event-stream");

        validatorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_CODE_HEADER, 400);
        verify(exchange).setException(any(AuthorizationException.class));
    }

    @Test
    void normalContentType_noException() throws Exception {
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Constants.CONTENT_TYPE)).thenReturn("application/json");
        when(message.getHeader(Constants.ACCEPT_TYPE)).thenReturn("application/json");

        validatorUnderTest.process(exchange);

        verify(exchange, never()).setException(any());
        verify(message, never()).setHeader(eq(Constants.REASON_CODE_HEADER), anyInt());
    }

    @Test
    void nullHeaders_noException() throws Exception {
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Constants.CONTENT_TYPE)).thenReturn(null);
        when(message.getHeader("content-type")).thenReturn(null);
        when(message.getHeader(Constants.ACCEPT_TYPE)).thenReturn(null);
        when(message.getHeader("accept")).thenReturn(null);

        validatorUnderTest.process(exchange);

        verify(exchange, never()).setException(any());
        verify(message, never()).setHeader(eq(Constants.REASON_CODE_HEADER), anyInt());
    }
}
