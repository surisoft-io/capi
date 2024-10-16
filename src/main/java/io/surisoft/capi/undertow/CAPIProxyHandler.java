package io.surisoft.capi.undertow;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.attribute.ExchangeAttribute;
import io.undertow.client.*;
import io.undertow.client.http2.Http2ClientConnection;
import io.undertow.connector.ByteBufferPool;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.predicate.IdempotentPredicate;
import io.undertow.predicate.Predicate;
import io.undertow.server.*;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.server.protocol.http.HttpAttachments;
import io.undertow.server.protocol.http.HttpContinue;
import io.undertow.util.*;
import org.jboss.logging.Logger;
import org.xnio.*;
import org.xnio.channels.StreamSinkChannel;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.undertow.client.http2.Http2ClearClientProvider.createSettingsFrame;

public final class CAPIProxyHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(CAPIProxyHandler.class);
    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = Integer.getInteger("io.undertow.server.handlers.proxy.maxRetries", 1);
    private static final AttachmentKey<ProxyConnection> CONNECTION = AttachmentKey.create(ProxyConnection.class);
    private static final AttachmentKey<HttpServerExchange> EXCHANGE = AttachmentKey.create(HttpServerExchange.class);
    private static final AttachmentKey<XnioExecutor.Key> TIMEOUT_KEY = AttachmentKey.create(XnioExecutor.Key.class);
    private final CAPILoadBalancerProxyClient proxyClient;
    private final int maxRequestTime;

    /**
     * Map of additional headers to add to the request.
     */
    private final Map<HttpString, ExchangeAttribute> requestHeaders = new CopyOnWriteMap<>();
    private final HttpHandler next;
    private final int maxConnectionRetries;
    private final Predicate idempotentRequestPredicate;

    public CAPIProxyHandler(CAPIProxyHandler.Builder builder) {
        this.proxyClient = builder.proxyClient;
        this.maxRequestTime = builder.maxRequestTime;
        this.next = builder.next;
        this.maxConnectionRetries = builder.maxConnectionRetries;
        this.idempotentRequestPredicate = builder.idempotentRequestPredicate;
        requestHeaders.putAll(builder.requestHeaders);
    }

    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final LoadBalancingProxyClient.ProxyTarget target = proxyClient.findTarget(exchange);

        if (target == null) {
            log.debugf("No proxy target for request to %s", exchange.getRequestURL());
            next.handleRequest(exchange);
            return;
        }
        if(exchange.isResponseStarted()) {
            //we can't proxy a request that has already started, this is basically a server configuration error
            UndertowLogger.REQUEST_LOGGER.cannotProxyStartedRequest(exchange);
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.endExchange();
            return;
        }
        final long timeout = maxRequestTime > 0 ? System.currentTimeMillis() + maxRequestTime : 0;
        int maxRetries = maxConnectionRetries;
        if(target instanceof ProxyClient.MaxRetriesProxyTarget) {
            maxRetries = Math.max(maxRetries, ((ProxyClient.MaxRetriesProxyTarget) target).getMaxRetries());
        }
        final CAPIProxyHandler.ProxyClientHandler clientHandler = new CAPIProxyHandler.ProxyClientHandler(exchange, target, timeout, maxRetries, idempotentRequestPredicate);
        if (timeout > 0) {
            final XnioExecutor.Key key = WorkerUtils.executeAfter(exchange.getIoThread(), () -> clientHandler.cancel(exchange), maxRequestTime, TimeUnit.MILLISECONDS);
            exchange.putAttachment(TIMEOUT_KEY, key);
            exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
                key.remove();
                nextListener.proceed();
            });
        }
        exchange.dispatch(exchange.isInIoThread() ? SameThreadExecutor.INSTANCE : exchange.getIoThread(), clientHandler);
    }

    static void copyHeaders(final HttpServerExchange exchange, final HeaderMap to, final HeaderMap from) {
        long f = from.fastIterateNonEmpty();
        HeaderValues values;
        while (f != -1L) {
            values = from.fiCurrent(f);
            if(!to.contains(values.getHeaderName())) {
                if (values.getHeaderName().toString().equals("HTTP2-Settings")) {
                    final OptionMap options = exchange.getConnection().getUndertowOptions();
                    final ByteBufferPool bufferPool = exchange.getConnection().getByteBufferPool();
                    to.put(new HttpString("HTTP2-Settings"), createSettingsFrame(options, bufferPool));
                } else {
                    //don't over write existing headers, normally the map will be empty, if it is not we assume it is not for a reason
                    to.putAll(values.getHeaderName(), values);
                }
            }
            f = from.fiNextNonEmpty(f);
        }
    }

    public ProxyClient getProxyClient() {
        return proxyClient;
    }

    @Override
    public String toString() {
        List<ProxyClient.ProxyTarget> proxyTargets = proxyClient.getAllTargets();
        if (proxyTargets.isEmpty()){
            return "ProxyHandler - "+proxyClient.getClass().getSimpleName();
        }
        if(proxyTargets.size()==1){
            return "reverse-proxy( '" + proxyTargets.get(0).toString() + "' )";
        } else {
            String outputResult = "reverse-proxy( { '" + proxyTargets.stream().map(Object::toString).collect(Collectors.joining("', '")) + "' }";
            return outputResult+" )";
        }
    }

    private final class ProxyClientHandler implements ProxyCallback<ProxyConnection>, Runnable {

        private int tries;

        private final long timeout;
        private final int maxRetryAttempts;
        private final HttpServerExchange exchange;
        private final Predicate idempotentPredicate;
        private ProxyClient.ProxyTarget target;

        ProxyClientHandler(HttpServerExchange exchange, ProxyClient.ProxyTarget target, long timeout, int maxRetryAttempts, Predicate idempotentPredicate) {
            this.exchange = exchange;
            this.timeout = timeout;
            this.maxRetryAttempts = maxRetryAttempts;
            this.target = target;
            this.idempotentPredicate = idempotentPredicate;
        }

        @Override
        public void run() {
            proxyClient.getConnection(target, exchange, this, -1, TimeUnit.MILLISECONDS);
        }

        @Override
        public void completed(final HttpServerExchange exchange, final ProxyConnection connection) {
            exchange.putAttachment(CONNECTION, connection);
            exchange.dispatch(SameThreadExecutor.INSTANCE, new CAPIProxyHandler.ProxyAction(connection, exchange, requestHeaders, exchange.isRequestComplete() ? this : null, idempotentPredicate));
        }

        @Override
        public void failed(final HttpServerExchange exchange) {
            final long time = System.currentTimeMillis();
            if (tries++ < maxRetryAttempts) {
                if (timeout > 0 && time > timeout) {
                    cancel(exchange);
                } else {
                    target = proxyClient.findTarget(exchange);
                    if (target != null) {
                        final long remaining = timeout > 0 ? timeout - time : -1;
                        proxyClient.getConnection(target, exchange, this, remaining, TimeUnit.MILLISECONDS);
                    } else {
                        couldNotResolveBackend(exchange); // The context was registered when we started, so return 503
                    }
                }
            } else {
                couldNotResolveBackend(exchange);
            }
        }

        @Override
        public void queuedRequestFailed(HttpServerExchange exchange) {
            failed(exchange);
        }

        @Override
        public void couldNotResolveBackend(HttpServerExchange exchange) {
            if (exchange.isResponseStarted()) {
                IoUtils.safeClose(exchange.getConnection());
            } else {
                exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
                exchange.endExchange();
            }
        }

        void cancel(final HttpServerExchange exchange) {
            //NOTE: this method is called only in context of timeouts.
            final ProxyConnection connectionAttachment = exchange.getAttachment(CONNECTION);
            if (connectionAttachment != null) {
                ClientConnection clientConnection = connectionAttachment.getConnection();
                UndertowLogger.PROXY_REQUEST_LOGGER.timingOutRequest(clientConnection.getPeerAddress() + "" + exchange.getRequestURI());
                IoUtils.safeClose(clientConnection);
            } else {
                UndertowLogger.PROXY_REQUEST_LOGGER.timingOutRequest(exchange.getRequestURI());
            }
            if (exchange.isResponseStarted()) {
                IoUtils.safeClose(exchange.getConnection());
            } else {
                exchange.setStatusCode(StatusCodes.GATEWAY_TIME_OUT);
                exchange.endExchange();
            }
        }

    }

    private record ProxyAction(ProxyConnection clientConnection, HttpServerExchange exchange,
                               Map<HttpString, ExchangeAttribute> requestHeaders, ProxyClientHandler proxyClientHandler,
                               Predicate idempotentPredicate) implements Runnable {

        @Override
            public void run() {
                final ClientRequest request = new ClientRequest();

                String targetURI = exchange.getRequestURI();
                if (exchange.isHostIncludedInRequestURI()) {
                    int uriPart = targetURI.indexOf("//");
                    if (uriPart != -1) {
                        uriPart = targetURI.indexOf("/", uriPart + 2);
                        if (uriPart != -1) {
                            targetURI = targetURI.substring(uriPart);
                        }
                    }
                }

                if (!exchange.getResolvedPath().isEmpty() && targetURI.startsWith(exchange.getResolvedPath())) {
                    targetURI = targetURI.substring(exchange.getResolvedPath().length());
                }

                StringBuilder requestURI = new StringBuilder();
                if (!clientConnection.getTargetPath().isEmpty()
                        && (!clientConnection.getTargetPath().equals("/") || targetURI.isEmpty())) {
                    requestURI.append(clientConnection.getTargetPath());
                }
                requestURI.append(targetURI);

                String qs = exchange.getQueryString();
                if (qs != null && !qs.isEmpty()) {
                    requestURI.append('?');
                    requestURI.append(qs);
                }
                request.setPath(requestURI.toString())
                        .setMethod(exchange.getRequestMethod());
                final HeaderMap inboundRequestHeaders = exchange.getRequestHeaders();
                final HeaderMap outboundRequestHeaders = request.getRequestHeaders();
                copyHeaders(exchange, outboundRequestHeaders, inboundRequestHeaders);

                if (!exchange.isPersistent()) {
                    //just because the client side is non-persistent
                    //we don't want to close the connection to the backend
                    outboundRequestHeaders.put(Headers.CONNECTION, "keep-alive");
                }
                if ("h2c".equals(exchange.getRequestHeaders().getFirst(Headers.UPGRADE))) {
                    //we don't allow h2c upgrade requests to be passed through to the backend
                    exchange.getRequestHeaders().remove(Headers.UPGRADE);
                    outboundRequestHeaders.put(Headers.CONNECTION, "keep-alive");
                }

                for (Map.Entry<HttpString, ExchangeAttribute> entry : requestHeaders.entrySet()) {
                    String headerValue = entry.getValue().readAttribute(exchange);
                    if (headerValue == null || headerValue.isEmpty()) {
                        outboundRequestHeaders.remove(entry.getKey());
                    } else {
                        outboundRequestHeaders.put(entry.getKey(), headerValue.replace('\n', ' '));
                    }
                }
                final String remoteHost;
                final InetSocketAddress address = exchange.getSourceAddress();
                if (address != null) {
                    remoteHost = address.getHostString();
                    if (!address.isUnresolved()) {
                        request.putAttachment(ProxiedRequestAttachments.REMOTE_ADDRESS, address.getAddress().getHostAddress());
                    }
                } else {
                    //should never happen, unless this is some form of mock request
                    remoteHost = "localhost";
                }

                request.putAttachment(ProxiedRequestAttachments.REMOTE_HOST, remoteHost);

                if (request.getRequestHeaders().contains(Headers.X_FORWARDED_FOR)) {
                    // We have an existing header so we shall simply append the host to the existing list
                    final String current = request.getRequestHeaders().getFirst(Headers.X_FORWARDED_FOR);
                    if (current == null || current.isEmpty()) {
                        // It was empty so just add it
                        request.getRequestHeaders().put(Headers.X_FORWARDED_FOR, remoteHost);
                    } else {
                        // Add the new entry and reset the existing header
                        request.getRequestHeaders().put(Headers.X_FORWARDED_FOR, current + "," + remoteHost);
                    }
                } else {
                    // No existing header or not allowed to reuse the header so set it here
                    request.getRequestHeaders().put(Headers.X_FORWARDED_FOR, remoteHost);
                }

                //if we don't support push set a header saying so
                //this is non standard, and a problem with the HTTP2 spec, but they did not want to listen
                if (!exchange.getConnection().isPushSupported() && clientConnection.getConnection().isPushSupported()) {
                    request.getRequestHeaders().put(Headers.X_DISABLE_PUSH, "true");
                }

                // Set the protocol header and attachment
                if (exchange.getRequestHeaders().contains(Headers.X_FORWARDED_PROTO)) {
                    final String proto = exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_PROTO);
                    request.putAttachment(ProxiedRequestAttachments.IS_SSL, proto.equals("https"));
                } else {
                    final String proto = exchange.getRequestScheme().equals("https") ? "https" : "http";
                    request.getRequestHeaders().put(Headers.X_FORWARDED_PROTO, proto);
                    request.putAttachment(ProxiedRequestAttachments.IS_SSL, proto.equals("https"));
                }

                // Set the server name
                if (exchange.getRequestHeaders().contains(Headers.X_FORWARDED_SERVER)) {
                    final String hostName = exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_SERVER);
                    request.putAttachment(ProxiedRequestAttachments.SERVER_NAME, hostName);
                } else {
                    final String hostName = exchange.getHostName();
                    request.getRequestHeaders().put(Headers.X_FORWARDED_SERVER, hostName);
                    request.putAttachment(ProxiedRequestAttachments.SERVER_NAME, hostName);
                }
                if (!exchange.getRequestHeaders().contains(Headers.X_FORWARDED_HOST)) {
                    final String hostName = exchange.getHostName();
                    if (hostName != null) {
                        request.getRequestHeaders().put(Headers.X_FORWARDED_HOST, NetworkUtils.formatPossibleIpv6Address(hostName));
                    }
                }

                // Set the port
                if (exchange.getRequestHeaders().contains(Headers.X_FORWARDED_PORT)) {
                    try {
                        int port = Integer.parseInt(exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_PORT));
                        request.putAttachment(ProxiedRequestAttachments.SERVER_PORT, port);
                    } catch (NumberFormatException e) {
                        int port = exchange.getConnection().getLocalAddress(InetSocketAddress.class).getPort();
                        request.getRequestHeaders().put(Headers.X_FORWARDED_PORT, port);
                        request.putAttachment(ProxiedRequestAttachments.SERVER_PORT, port);
                    }
                } else {
                    int port = exchange.getHostPort();
                    request.getRequestHeaders().put(Headers.X_FORWARDED_PORT, port);
                    request.putAttachment(ProxiedRequestAttachments.SERVER_PORT, port);
                }

                SSLSessionInfo sslSessionInfo = exchange.getConnection().getSslSessionInfo();
                if (sslSessionInfo != null) {
                    Certificate[] peerCertificates;
                    try {
                        peerCertificates = sslSessionInfo.getPeerCertificates();
                        if (peerCertificates.length > 0) {
                            request.putAttachment(ProxiedRequestAttachments.SSL_CERT, Certificates.toPem(peerCertificates[0]));
                        }
                    } catch (SSLPeerUnverifiedException | CertificateEncodingException | RenegotiationRequiredException e) {
                        //ignore
                    }
                    request.putAttachment(ProxiedRequestAttachments.SSL_CYPHER, sslSessionInfo.getCipherSuite());
                    request.putAttachment(ProxiedRequestAttachments.SSL_SESSION_ID, sslSessionInfo.getSessionId());
                    request.putAttachment(ProxiedRequestAttachments.SSL_KEY_SIZE, sslSessionInfo.getKeySize());
                }

                if (log.isDebugEnabled()) {
                    log.debugf("Sending request %s to target %s for exchange %s", request, clientConnection.getConnection().getPeerAddress(), exchange);
                }
                //handle content
                //if the frontend is HTTP/2 then we may need to add a Transfer-Encoding header, to indicate to the backend
                //that there is content
                if (!request.getRequestHeaders().contains(Headers.TRANSFER_ENCODING) && !request.getRequestHeaders().contains(Headers.CONTENT_LENGTH)) {
                    if (!exchange.isRequestComplete()) {
                        request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, Headers.CHUNKED.toString());
                    }
                }

                //https://www.rfc-editor.org/rfc/rfc9113#name-compressing-the-cookie-head
                if (!Cookies.isCrumbsAssemplyDisabled() && !(clientConnection.getConnection() instanceof Http2ClientConnection)) {
                    Cookies.assembleCrumbs(outboundRequestHeaders);
                }
                clientConnection.getConnection().sendRequest(request, new ClientCallback<>() {
                    @Override
                    public void completed(final ClientExchange result) {

                        if (log.isDebugEnabled()) {
                            log.debugf("Sent request %s to target %s for exchange %s", request, remoteHost, exchange);
                        }
                        result.putAttachment(EXCHANGE, exchange);

                        boolean requiresContinueResponse = HttpContinue.requiresContinueResponse(exchange);
                        if (requiresContinueResponse) {
                            result.setContinueHandler(new ContinueNotification() {
                                @Override
                                public void handleContinue(final ClientExchange clientExchange) {
                                    if (log.isDebugEnabled()) {
                                        log.debugf("Received continue response to request %s to target %s for exchange %s", request, clientConnection.getConnection().getPeerAddress(), exchange);
                                    }
                                    HttpContinue.sendContinueResponse(exchange, new IoCallback() {
                                        @Override
                                        public void onComplete(final HttpServerExchange exchange, final Sender sender) {
                                            //don't care
                                        }

                                        @Override
                                        public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
                                            IoUtils.safeClose(clientConnection.getConnection());
                                            exchange.endExchange();
                                            UndertowLogger.REQUEST_IO_LOGGER.ioException(exception);
                                        }
                                    });
                                }
                            });
                        }

                        //handle server push
                        if (exchange.getConnection().isPushSupported() && result.getConnection().isPushSupported()) {
                            result.setPushHandler(new PushCallback() {
                                @Override
                                public boolean handlePush(ClientExchange originalRequest, final ClientExchange pushedRequest) {

                                    if (log.isDebugEnabled()) {
                                        log.debugf("Sending push request %s received from %s to target %s for exchange %s", pushedRequest.getRequest(), request, remoteHost, exchange);
                                    }
                                    final ClientRequest request = pushedRequest.getRequest();
                                    exchange.getConnection().pushResource(request.getPath(), request.getMethod(), request.getRequestHeaders(), new HttpHandler() {
                                        @Override
                                        public void handleRequest(final HttpServerExchange exchange) throws Exception {
                                            String path = request.getPath();
                                            int i = path.indexOf("?");
                                            if (i > 0) {
                                                path = path.substring(0, i);
                                            }

                                            exchange.dispatch(SameThreadExecutor.INSTANCE, new CAPIProxyHandler.ProxyAction(new ProxyConnection(pushedRequest.getConnection(), path), exchange, requestHeaders, null, idempotentPredicate));
                                        }
                                    });
                                    return true;
                                }
                            });
                        }


                        result.setResponseListener(new ResponseCallback(exchange, proxyClientHandler, idempotentPredicate));
                        final IoExceptionHandler handler = new IoExceptionHandler(exchange, clientConnection.getConnection());
                        if (requiresContinueResponse) {
                            try {
                                if (!result.getRequestChannel().flush()) {
                                    result.getRequestChannel().getWriteSetter().set(ChannelListeners.flushingChannelListener(new ChannelListener<StreamSinkChannel>() {
                                        @Override
                                        public void handleEvent(StreamSinkChannel channel) {
                                            Transfer.initiateTransfer(exchange.getRequestChannel(), result.getRequestChannel(), ChannelListeners.closingChannelListener(), new HTTPTrailerChannelListener(exchange, result, exchange, proxyClientHandler, idempotentPredicate), handler, handler, exchange.getConnection().getByteBufferPool());

                                        }
                                    }, handler));
                                    result.getRequestChannel().resumeWrites();
                                    return;
                                }
                            } catch (IOException e) {
                                handler.handleException(result.getRequestChannel(), e);
                            }
                        }
                        HTTPTrailerChannelListener trailerListener = new HTTPTrailerChannelListener(exchange, result, exchange, proxyClientHandler, idempotentPredicate);
                        if (!exchange.isRequestComplete()) {
                            Transfer.initiateTransfer(exchange.getRequestChannel(), result.getRequestChannel(), ChannelListeners.closingChannelListener(), trailerListener, handler, handler, exchange.getConnection().getByteBufferPool());
                        } else {
                            trailerListener.handleEvent(result.getRequestChannel());
                        }

                    }

                    @Override
                    public void failed(IOException e) {
                        handleFailure(exchange, proxyClientHandler, idempotentPredicate, e);
                    }
                });


            }
        }

    static void handleFailure(HttpServerExchange exchange, CAPIProxyHandler.ProxyClientHandler proxyClientHandler, Predicate idempotentRequestPredicate, IOException e) {
        UndertowLogger.PROXY_REQUEST_LOGGER.proxyRequestFailed(exchange.getRequestURI(), e);
        if(exchange.isResponseStarted()) {
            IoUtils.safeClose(exchange.getConnection());
        } else if(idempotentRequestPredicate.resolve(exchange) && proxyClientHandler != null) {
            proxyClientHandler.failed(exchange); //this will attempt a retry if configured to do so
        } else {
            exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
            exchange.endExchange();
        }
    }

    private static final class ResponseCallback implements ClientCallback<ClientExchange> {

        private final HttpServerExchange exchange;
        private final CAPIProxyHandler.ProxyClientHandler proxyClientHandler;
        private final Predicate idempotentPredicate;

        private ResponseCallback(HttpServerExchange exchange, CAPIProxyHandler.ProxyClientHandler proxyClientHandler, Predicate idempotentPredicate) {
            this.exchange = exchange;
            this.proxyClientHandler = proxyClientHandler;
            this.idempotentPredicate = idempotentPredicate;
        }

        @Override
        public void completed(final ClientExchange result) {

            final ClientResponse response = result.getResponse();

            if(log.isDebugEnabled()) {
                log.debugf("Received response %s for request %s for exchange %s", response, result.getRequest(), exchange);
            }
            final HeaderMap inboundResponseHeaders = response.getResponseHeaders();
            final HeaderMap outboundResponseHeaders = exchange.getResponseHeaders();
            exchange.setStatusCode(response.getResponseCode());
            copyHeaders(exchange, outboundResponseHeaders, inboundResponseHeaders);

            //https://www.rfc-editor.org/rfc/rfc9113#name-compressing-the-cookie-head
            //NOTE: this will be required if this is passed into app
            if(!Cookies.isCrumbsAssemplyDisabled() && !exchange.getProtocol().equals(Protocols.HTTP_2_0)) {
                Cookies.assembleCrumbs(outboundResponseHeaders);
            }

            if (exchange.isUpgrade()) {

                exchange.upgradeChannel(new HttpUpgradeListener() {
                    @Override
                    public void handleUpgrade(StreamConnection streamConnection, HttpServerExchange exchange) {

                        if(log.isDebugEnabled()) {
                            log.debugf("Upgraded request %s to for exchange %s", result.getRequest(), exchange);
                        }
                        StreamConnection clientChannel = null;
                        try {
                            clientChannel = result.getConnection().performUpgrade();

                            final CAPIProxyHandler.ClosingExceptionHandler handler = new CAPIProxyHandler.ClosingExceptionHandler(streamConnection, clientChannel);
                            Transfer.initiateTransfer(clientChannel.getSourceChannel(), streamConnection.getSinkChannel(), ChannelListeners.closingChannelListener(), ChannelListeners.writeShutdownChannelListener(ChannelListeners.<StreamSinkChannel>flushingChannelListener(ChannelListeners.closingChannelListener(), ChannelListeners.closingChannelExceptionHandler()), ChannelListeners.closingChannelExceptionHandler()), handler, handler, result.getConnection().getBufferPool());
                            Transfer.initiateTransfer(streamConnection.getSourceChannel(), clientChannel.getSinkChannel(), ChannelListeners.closingChannelListener(), ChannelListeners.writeShutdownChannelListener(ChannelListeners.<StreamSinkChannel>flushingChannelListener(ChannelListeners.closingChannelListener(), ChannelListeners.closingChannelExceptionHandler()), ChannelListeners.closingChannelExceptionHandler()), handler, handler, result.getConnection().getBufferPool());

                        } catch (IOException e) {
                            IoUtils.safeClose(streamConnection, clientChannel);
                        }
                    }
                });
            }
            final CAPIProxyHandler.IoExceptionHandler handler = new CAPIProxyHandler.IoExceptionHandler(exchange, result.getConnection());
            Transfer.initiateTransfer(result.getResponseChannel(), exchange.getResponseChannel(), ChannelListeners.closingChannelListener(), new CAPIProxyHandler.HTTPTrailerChannelListener(result, exchange, exchange, proxyClientHandler, idempotentPredicate), handler, handler, exchange.getConnection().getByteBufferPool());
        }

        @Override
        public void failed(IOException e) {
            handleFailure(exchange, proxyClientHandler, idempotentPredicate, e);
        }
    }

    private static final class HTTPTrailerChannelListener implements ChannelListener<StreamSinkChannel> {

        private final Attachable source;
        private final Attachable target;
        private final HttpServerExchange exchange;
        private final CAPIProxyHandler.ProxyClientHandler proxyClientHandler;
        private final Predicate idempotentPredicate;

        private HTTPTrailerChannelListener(final Attachable source, final Attachable target, HttpServerExchange exchange, CAPIProxyHandler.ProxyClientHandler proxyClientHandler, Predicate idempotentPredicate) {
            this.source = source;
            this.target = target;
            this.exchange = exchange;
            this.proxyClientHandler = proxyClientHandler;
            this.idempotentPredicate = idempotentPredicate;
        }

        @Override
        public void handleEvent(final StreamSinkChannel channel) {
            HeaderMap trailers = source.getAttachment(HttpAttachments.REQUEST_TRAILERS);
            if (trailers != null) {
                target.putAttachment(HttpAttachments.RESPONSE_TRAILERS, trailers);
            }
            try {
                channel.shutdownWrites();
                if (!channel.flush()) {
                    channel.getWriteSetter().set(ChannelListeners.flushingChannelListener(new ChannelListener<StreamSinkChannel>() {
                        @Override
                        public void handleEvent(StreamSinkChannel channel) {
                            channel.suspendWrites();
                            channel.getWriteSetter().set(null);
                        }
                    }, ChannelListeners.closingChannelExceptionHandler()));
                    channel.resumeWrites();
                } else {
                    channel.getWriteSetter().set(null);
                    channel.shutdownWrites();
                }
            } catch (IOException e) {
                handleFailure(exchange, proxyClientHandler, idempotentPredicate, e);
            } catch (Exception e) {
                handleFailure(exchange, proxyClientHandler, idempotentPredicate, new IOException(e));
            }

        }
    }

    private static final class IoExceptionHandler implements ChannelExceptionHandler<Channel> {

        private final HttpServerExchange exchange;
        private final ClientConnection clientConnection;

        private IoExceptionHandler(HttpServerExchange exchange, ClientConnection clientConnection) {
            this.exchange = exchange;
            this.clientConnection = clientConnection;
        }

        @Override
        public void handleException(Channel channel, IOException exception) {
            IoUtils.safeClose(channel);
            IoUtils.safeClose(clientConnection);
            if (exchange.isResponseStarted()) {
                UndertowLogger.REQUEST_IO_LOGGER.debug("Exception reading from target server", exception);
                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.endExchange();
                } else {
                    IoUtils.safeClose(exchange.getConnection());
                }
            } else {
                UndertowLogger.REQUEST_IO_LOGGER.ioException(exception);
                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                exchange.endExchange();
            }
        }
    }

    private record ClosingExceptionHandler(Closeable... toClose) implements ChannelExceptionHandler<Channel> {


        @Override
            public void handleException(Channel channel, IOException exception) {
                IoUtils.safeClose(channel);
                IoUtils.safeClose(toClose);
            }
        }

    public static CAPIProxyHandler.Builder builder() {
        return new CAPIProxyHandler.Builder();
    }

    public static class Builder {

        private CAPILoadBalancerProxyClient proxyClient;
        private int maxRequestTime = -1;
        private final Map<HttpString, ExchangeAttribute> requestHeaders = new CopyOnWriteMap<>();
        private HttpHandler next = ResponseCodeHandler.HANDLE_404;
        private final int maxConnectionRetries = DEFAULT_MAX_RETRY_ATTEMPTS;
        private final Predicate idempotentRequestPredicate = IdempotentPredicate.INSTANCE;

        Builder() {}

        public CAPIProxyHandler.Builder setProxyClient(CAPILoadBalancerProxyClient proxyClient) {
            if(proxyClient == null) {
                throw UndertowMessages.MESSAGES.argumentCannotBeNull("proxyClient");
            }
            this.proxyClient = proxyClient;
            return this;
        }

        public CAPIProxyHandler.Builder setMaxRequestTime(int maxRequestTime) {
            this.maxRequestTime = maxRequestTime;
            return this;
        }

        public CAPIProxyHandler.Builder setNext(HttpHandler next) {
            this.next = next;
            return this;
        }

        public CAPIProxyHandler build() {
            return new CAPIProxyHandler(this);
        }
    }
}