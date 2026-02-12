package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.service.OpaService;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.cache2k.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationProcessorTest {

    @Mock
    private HttpUtils httpUtils;
    @Mock
    private Cache<String, Service> serviceCache;
    @Mock
    private OpaService opaService;
    @Mock
    private Exchange exchange;
    @Mock
    private Message message;

    private AuthorizationProcessor processorUnderTest;

    @BeforeEach
    void setUp() {
        processorUnderTest = new AuthorizationProcessor(httpUtils, serviceCache, Optional.of(opaService));
    }

    @Test
    void validTokenAuthorized_propagatesAuthAndPreparesThrottle() throws Exception {
        String contextPath = "/test/dev";
        String accessToken = "valid-token";
        Service service = new Service();

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH)).thenReturn(contextPath);
        when(httpUtils.processAuthorizationAccessToken(exchange)).thenReturn(accessToken);
        when(httpUtils.contextToRole(contextPath)).thenReturn("test:dev");
        when(serviceCache.get("test:dev")).thenReturn(service);
        when(httpUtils.isAuthorized(eq(accessToken), eq(contextPath), eq(service), eq(opaService))).thenReturn(true);

        processorUnderTest.process(exchange);

        verify(httpUtils).propagateAuthorization(exchange, accessToken);
        verify(httpUtils).prepareForThrottleIfNeeded(service, accessToken, exchange);
        verify(exchange, never()).setException(any());
    }

    @Test
    void validTokenNotAuthorized_returns401NotSubscribed() throws Exception {
        String contextPath = "/test/dev";
        String accessToken = "valid-token";
        Service service = new Service();

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH)).thenReturn(contextPath);
        when(httpUtils.processAuthorizationAccessToken(exchange)).thenReturn(accessToken);
        when(httpUtils.contextToRole(contextPath)).thenReturn("test:dev");
        when(serviceCache.get("test:dev")).thenReturn(service);
        when(httpUtils.isAuthorized(eq(accessToken), eq(contextPath), eq(service), eq(opaService))).thenReturn(false);

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_CODE_HEADER, HttpStatus.UNAUTHORIZED.value());
        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "Not subscribed");
        verify(exchange).setException(any(AuthorizationException.class));
    }

    @Test
    void nullToken_returns401NoAuthorizationHeader() throws Exception {
        String contextPath = "/test/dev";
        Service service = new Service();

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH)).thenReturn(contextPath);
        when(httpUtils.processAuthorizationAccessToken(exchange)).thenReturn(null);
        when(httpUtils.contextToRole(contextPath)).thenReturn("test:dev");
        when(serviceCache.get("test:dev")).thenReturn(service);

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "No authorization header provided");
        verify(message).setHeader(Constants.REASON_CODE_HEADER, HttpStatus.UNAUTHORIZED.value());
        verify(exchange).setException(any(AuthorizationException.class));
    }

    @Test
    void authorizationExceptionThrown_returns401WithCauseMessage() throws Exception {
        String contextPath = "/test/dev";

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH)).thenReturn(contextPath);
        when(httpUtils.processAuthorizationAccessToken(exchange)).thenThrow(new AuthorizationException("Token expired"));

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "Token expired");
        verify(message).setHeader(Constants.REASON_CODE_HEADER, HttpStatus.UNAUTHORIZED.value());
        verify(exchange).setException(any(AuthorizationException.class));
    }

    @Test
    void parseExceptionThrown_returns401WithCauseMessage() throws Exception {
        String contextPath = "/test/dev";
        Service service = new Service();

        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH)).thenReturn(contextPath);
        when(httpUtils.processAuthorizationAccessToken(exchange)).thenReturn("valid-token");
        when(httpUtils.contextToRole(contextPath)).thenReturn("test:dev");
        when(serviceCache.get("test:dev")).thenReturn(service);
        when(httpUtils.isAuthorized(anyString(), anyString(), any(), any()))
                .thenReturn(true);
        doThrow(new ParseException("Bad token format", 0))
                .when(httpUtils).prepareForThrottleIfNeeded(any(), anyString(), any());

        processorUnderTest.process(exchange);

        verify(message).setHeader(Constants.REASON_MESSAGE_HEADER, "Bad token format");
        verify(message).setHeader(Constants.REASON_CODE_HEADER, HttpStatus.UNAUTHORIZED.value());
        verify(exchange).setException(any(AuthorizationException.class));
    }
}
