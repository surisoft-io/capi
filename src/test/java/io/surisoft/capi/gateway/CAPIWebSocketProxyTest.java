package io.surisoft.capi.gateway;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.CloseStatus;
import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.client.WebSocketCoreClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CAPIWebSocketProxyTest {

    @Mock
    private WebSocketCoreClient mockWsClient;
    @Mock
    private CoreSession mockServerSession;
    @Mock
    private Frame mockFrame;
    @Mock
    private Callback mockCallback;

    private CAPIWebSocketProxy proxyUnderTest;

    @BeforeEach
    void setUp() {
        proxyUnderTest = new CAPIWebSocketProxy(URI.create("ws://backend:8080/ws"), mockWsClient);
    }

    @Test
    void testOnFrameSucceedsCallbackWhenServerSessionNull() {
        // serverSession is null by default (onOpen not called)
        proxyUnderTest.onFrame(mockFrame, mockCallback);
        verify(mockCallback).succeeded();
    }

    @Test
    void testOnClosedClosesServerSessionWhenOpen() throws Exception {
        setServerSession(mockServerSession);
        when(mockServerSession.isOutputOpen()).thenReturn(true);

        CloseStatus closeStatus = new CloseStatus(CloseStatus.NORMAL);
        proxyUnderTest.onClosed(closeStatus, mockCallback);

        verify(mockServerSession).close(closeStatus, mockCallback);
    }

    @Test
    void testOnClosedSucceedsCallbackWhenServerSessionNull() {
        proxyUnderTest.onClosed(new CloseStatus(CloseStatus.NORMAL), mockCallback);
        verify(mockCallback).succeeded();
    }

    @Test
    void testOnErrorClosesServerSessionWithServerError() throws Exception {
        setServerSession(mockServerSession);
        when(mockServerSession.isOutputOpen()).thenReturn(true);

        proxyUnderTest.onError(new RuntimeException("test error"), mockCallback);

        ArgumentCaptor<CloseStatus> closeStatusCaptor = ArgumentCaptor.forClass(CloseStatus.class);
        verify(mockServerSession).close(closeStatusCaptor.capture(), eq(Callback.NOOP));
        assertThat(closeStatusCaptor.getValue().getCode()).isEqualTo(CloseStatus.SERVER_ERROR);
        verify(mockCallback).succeeded();
    }

    @Test
    void testOnErrorSucceedsCallbackWhenServerSessionNull() {
        proxyUnderTest.onError(new RuntimeException("test error"), mockCallback);
        verify(mockCallback).succeeded();
        verifyNoInteractions(mockServerSession);
    }

    private void setServerSession(CoreSession session) throws Exception {
        Field serverSessionField = CAPIWebSocketProxy.class.getDeclaredField("serverSession");
        serverSessionField.setAccessible(true);
        serverSessionField.set(proxyUnderTest, session);
    }
}
