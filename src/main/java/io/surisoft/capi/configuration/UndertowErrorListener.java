package io.surisoft.capi.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.schema.CapiRestError;
import io.surisoft.capi.utils.Constants;
import io.undertow.Undertow;
import io.undertow.util.HeaderMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "capi.gateway.error.listener", name = "enabled", havingValue = "true")
public class UndertowErrorListener {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int listenerPort;
    private final String listenerContext;

    public UndertowErrorListener(@Value("${capi.gateway.error.listener.port}") int listenerPort,
                                @Value("${capi.gateway.error.listener.context}") String listenerContext) {
        this.listenerPort = listenerPort;
        this.listenerContext = listenerContext;
        runProxy();
    }

    public void runProxy() {
        Undertow undertow = Undertow.builder()
                .addHttpListener(listenerPort, Constants.ERROR_LISTENING_ADDRESS)
                .setHandler(httpServerExchange -> {
                    if(httpServerExchange.getRelativePath().startsWith(listenerContext)) {
                        httpServerExchange.getResponseHeaders().add(Constants.HTTP_STRING_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                        HeaderMap headerMap = httpServerExchange.getRequestHeaders();
                        CapiRestError capiRestError = buildCapiErrorObject(headerMap);

                        if(headerMap.contains(Constants.REASON_CODE_HEADER)) {
                            httpServerExchange.setStatusCode(Integer.parseInt(headerMap.get(Constants.REASON_CODE_HEADER).get(0)));
                        }
                        httpServerExchange.getResponseSender().send(objectMapper.writeValueAsString(capiRestError));
                        httpServerExchange.endExchange();
                    } else {
                        httpServerExchange.setStatusCode(400);
                        httpServerExchange.getResponseSender().send("BAD REQUEST");
                        httpServerExchange.endExchange();
                    }
                }).build();
        undertow.start();
    }

    private CapiRestError buildCapiErrorObject(HeaderMap headerMap) {
        CapiRestError capiRestError = new CapiRestError();
        if(headerMap.contains(Constants.REASON_CODE_HEADER)) {
            capiRestError.setErrorCode(Integer.parseInt(headerMap.get(Constants.REASON_CODE_HEADER).get(0)));
        }

        if(headerMap.contains(Constants.REASON_MESSAGE_HEADER)) {
            capiRestError.setErrorMessage(headerMap.get(Constants.REASON_MESSAGE_HEADER).get(0));
        }

        if(headerMap.contains(Constants.ROUTE_ID_HEADER)) {
            capiRestError.setRouteID(headerMap.get(Constants.ROUTE_ID_HEADER).get(0));
        }

        if(headerMap.contains(Constants.CAPI_URI_IN_ERROR)) {
            capiRestError.setHttpUri(headerMap.get(Constants.CAPI_URI_IN_ERROR).get(0));
        }

        if(headerMap.contains(Constants.TRACE_ID_HEADER)) {
            capiRestError.setTraceID(headerMap.get(Constants.TRACE_ID_HEADER).get(0));
        }

        return capiRestError;
    }
}