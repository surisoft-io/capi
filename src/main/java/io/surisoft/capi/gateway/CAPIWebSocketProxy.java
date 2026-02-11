package io.surisoft.capi.gateway;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.CloseStatus;
import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.FrameHandler;
import org.eclipse.jetty.websocket.core.client.WebSocketCoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * WebSocket proxy that bridges a client WebSocket connection to a backend WebSocket connection.
 * Frames received from the client are forwarded to the backend, and vice versa.
 */
public class CAPIWebSocketProxy implements FrameHandler {

    private static final Logger log = LoggerFactory.getLogger(CAPIWebSocketProxy.class);

    private final URI backendUri;
    private final WebSocketCoreClient wsClient;
    private CoreSession clientSession;
    private CoreSession serverSession;

    public CAPIWebSocketProxy(URI backendUri, WebSocketCoreClient wsClient) {
        this.backendUri = backendUri;
        this.wsClient = wsClient;
    }

    @Override
    public void onOpen(CoreSession session, Callback callback) {
        this.clientSession = session;
        log.debug("Client WebSocket opened, connecting to backend: {}", backendUri);

        BackendFrameHandler backendHandler = new BackendFrameHandler();
        try {
            wsClient.connect(backendHandler, backendUri)
                    .whenComplete((serverCoreSession, error) -> {
                        if (error != null) {
                            log.error("Failed to connect to backend WebSocket: {}", backendUri, error);
                            callback.failed(error);
                        } else {
                            this.serverSession = serverCoreSession;
                            log.debug("Backend WebSocket connected: {}", backendUri);
                            callback.succeeded();
                            clientSession.demand();
                        }
                    });
        } catch (Exception e) {
            log.error("Error initiating backend WebSocket connection: {}", backendUri, e);
            callback.failed(e);
        }
    }

    @Override
    public void onFrame(Frame frame, Callback callback) {
        if (serverSession == null || !serverSession.isOutputOpen()) {
            callback.succeeded();
            return;
        }
        Frame copy = Frame.copy(frame);
        serverSession.sendFrame(copy, Callback.from(() -> {
            callback.succeeded();
            clientSession.demand();
        }, failure -> {
            log.debug("Failed to forward frame to backend", failure);
            callback.failed(failure);
        }), false);
    }

    @Override
    public void onClosed(CloseStatus closeStatus, Callback callback) {
        log.debug("Client WebSocket closed: {}", closeStatus);
        if (serverSession != null && serverSession.isOutputOpen()) {
            serverSession.close(closeStatus, callback);
        } else {
            callback.succeeded();
        }
    }

    @Override
    public void onError(Throwable cause, Callback callback) {
        log.debug("Client WebSocket error", cause);
        if (serverSession != null && serverSession.isOutputOpen()) {
            serverSession.close(new CloseStatus(CloseStatus.SERVER_ERROR), Callback.NOOP);
        }
        callback.succeeded();
    }

    /**
     * Handles frames coming from the backend and forwards them to the client.
     */
    private class BackendFrameHandler implements FrameHandler {

        @Override
        public void onOpen(CoreSession session, Callback callback) {
            serverSession = session;
            callback.succeeded();
            session.demand();
        }

        @Override
        public void onFrame(Frame frame, Callback callback) {
            if (clientSession == null || !clientSession.isOutputOpen()) {
                callback.succeeded();
                return;
            }
            Frame copy = Frame.copy(frame);
            clientSession.sendFrame(copy, Callback.from(() -> {
                callback.succeeded();
                serverSession.demand();
            }, failure -> {
                log.debug("Failed to forward frame to client", failure);
                callback.failed(failure);
            }), false);
        }

        @Override
        public void onClosed(CloseStatus closeStatus, Callback callback) {
            log.debug("Backend WebSocket closed: {}", closeStatus);
            if (clientSession != null && clientSession.isOutputOpen()) {
                clientSession.close(closeStatus, callback);
            } else {
                callback.succeeded();
            }
        }

        @Override
        public void onError(Throwable cause, Callback callback) {
            log.debug("Backend WebSocket error", cause);
            if (clientSession != null && clientSession.isOutputOpen()) {
                clientSession.close(new CloseStatus(CloseStatus.SERVER_ERROR), Callback.NOOP);
            }
            callback.succeeded();
        }
    }
}
