package io.surisoft.capi.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.schema.CapiRestError;
import io.surisoft.capi.utils.Constants;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "capi.gateway.error.listener", name = "enabled", havingValue = "true")
public class JettyErrorListener {
    private static final Logger log = LoggerFactory.getLogger(JettyErrorListener.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int listenerPort;
    private final String listenerContext;

    public JettyErrorListener(@Value("${capi.gateway.error.listener.port}") int listenerPort,
                              @Value("${capi.gateway.error.listener.context}") String listenerContext) {
        this.listenerPort = listenerPort;
        this.listenerContext = listenerContext;
        runProxy();
    }

    public void runProxy() {
        try {
            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(listenerPort);
            server.addConnector(connector);

            server.setHandler(new Handler.Abstract() {
                @Override
                public boolean handle(Request request, Response response, Callback callback) throws Exception {
                    String path = Request.getPathInContext(request);
                    if (path.startsWith(listenerContext)) {
                        response.getHeaders().put(Constants.CONTENT_TYPE_HEADER, MediaType.APPLICATION_JSON_VALUE);
                        HttpFields requestHeaders = request.getHeaders();
                        CapiRestError capiRestError = buildCapiErrorObject(requestHeaders);

                        if (requestHeaders.contains(Constants.REASON_CODE_HEADER)) {
                            response.setStatus(Integer.parseInt(requestHeaders.get(Constants.REASON_CODE_HEADER)));
                        }
                        Content.Sink.write(response, true, objectMapper.writeValueAsString(capiRestError), callback);
                    } else {
                        response.setStatus(400);
                        Content.Sink.write(response, true, "BAD REQUEST", callback);
                    }
                    return true;
                }
            });
            server.start();
        } catch (Exception e) {
            log.error("Failed to start error listener", e);
        }
    }

    CapiRestError buildCapiErrorObject(HttpFields headers) {
        CapiRestError capiRestError = new CapiRestError();
        if (headers.contains(Constants.REASON_CODE_HEADER)) {
            capiRestError.setErrorCode(Integer.parseInt(headers.get(Constants.REASON_CODE_HEADER)));
        }

        if (headers.contains(Constants.REASON_MESSAGE_HEADER)) {
            capiRestError.setErrorMessage(headers.get(Constants.REASON_MESSAGE_HEADER));
        }

        if (headers.contains(Constants.ROUTE_ID_HEADER)) {
            capiRestError.setRouteID(headers.get(Constants.ROUTE_ID_HEADER));
        }

        if (headers.contains(Constants.CAPI_URI_IN_ERROR)) {
            capiRestError.setHttpUri(headers.get(Constants.CAPI_URI_IN_ERROR));
        }

        if (headers.contains(Constants.TRACE_ID_HEADER)) {
            capiRestError.setTraceID(headers.get(Constants.TRACE_ID_HEADER));
        }

        return capiRestError;
    }
}
