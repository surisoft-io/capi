package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpErrorProcessorTest {

    @Mock
    private Exchange exchange;
    @Mock
    private Message message;

    private HttpErrorProcessor processorUnderTest;

    @BeforeEach
    void setUp() {
        processorUnderTest = new HttpErrorProcessor();
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Exchange.HTTP_URI)).thenReturn("http://backend:8080/api");
        when(message.getHeader(Exchange.HTTP_URL)).thenReturn("http://backend:8080/api?param=1");
    }

    @Test
    void sslHandshakeException_sets502WithCertificateMessage() {
        SSLHandshakeException cause = new SSLHandshakeException("cert error");
        when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class)).thenReturn(cause);

        processorUnderTest.process(exchange);

        // SSLHandshakeException extends SSLException, so both if-blocks match
        verify(message, atLeast(1)).setHeader(Constants.REASON_MESSAGE_HEADER, "Problem with Service certificate");
        verify(message, atLeast(1)).setHeader(Constants.REASON_CODE_HEADER, 502);
        verify(message).setHeader(Constants.CAPI_URI_IN_ERROR, "http://backend:8080/api");
        verify(message).setHeader(Constants.CAPI_URL_IN_ERROR, "http://backend:8080/api?param=1");
    }

    @Test
    void unknownHostException_sets502WithHostMessage() {
        UnknownHostException cause = new UnknownHostException("unknown-host");
        when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class)).thenReturn(cause);

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "Problem with Service host");
        verify(message).setHeader(Constants.REASON_CODE_HEADER, 502);
    }

    @Test
    void socketTimeoutException_sets502WithTimeoutMessage() {
        SocketTimeoutException cause = new SocketTimeoutException("timed out");
        when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class)).thenReturn(cause);

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "The remote server took too long");
        verify(message).setHeader(Constants.REASON_CODE_HEADER, 502);
    }

    @Test
    void httpHostConnectException_sets502WithNoServerMessage() {
        HttpHostConnectException cause = new HttpHostConnectException("Connection refused");
        when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class)).thenReturn(cause);

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "No server available at the moment. Please try again later.");
        verify(message).setHeader(Constants.REASON_CODE_HEADER, 502);
    }

    @Test
    void authorizationException_sets401WithExceptionMessage() {
        AuthorizationException cause = new AuthorizationException("Not authorized");
        when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class)).thenReturn(cause);

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "Not authorized");
        verify(message).setHeader(Constants.REASON_CODE_HEADER, 401);
    }

    @Test
    void storesHttpUriAndHttpUrlInCapiErrorHeaders() {
        when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class)).thenReturn(null);

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.CAPI_URI_IN_ERROR, "http://backend:8080/api");
        verify(message).setHeader(Constants.CAPI_URL_IN_ERROR, "http://backend:8080/api?param=1");
    }
}
