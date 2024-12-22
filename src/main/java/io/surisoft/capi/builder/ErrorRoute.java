package io.surisoft.capi.builder;

import io.surisoft.capi.schema.CapiRestError;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ErrorRoute extends RouteBuilder {

    private final HttpUtils httpUtils;

    public ErrorRoute(HttpUtils httpUtils) {
        this.httpUtils = httpUtils;
    }

    @Override
    public void configure() {
        from("direct:error")
                .setHeader("Content-Type", constant("application/json"))
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        CapiRestError capiRestError  = new CapiRestError();
                        if(exchange.getIn().getHeader(Constants.CAPI_URI_IN_ERROR) != null) {
                            capiRestError.setHttpUri(exchange.getIn().getHeader(Constants.CAPI_URI_IN_ERROR, String.class));
                        }
                        if(exchange.getIn().getHeader(Constants.ROUTE_ID_HEADER) != null) {
                            capiRestError.setRouteID(exchange.getIn().getHeader(Constants.ROUTE_ID_HEADER, String.class));
                        }
                        if(exchange.getIn().getHeader("x-b3-traceid") != null || exchange.getIn().getHeader("X-B3-Traceid", String.class) != null) {
                            capiRestError.setTraceID(exchange.getIn().getHeader(Constants.TRACE_ID_HEADER, String.class));
                        }
                        if(exchange.getIn().getHeader(Constants.REASON_MESSAGE_HEADER) != null && exchange.getIn().getHeader(Constants.REASON_CODE_HEADER) != null) {
                            capiRestError.setErrorMessage(exchange.getIn().getHeader(Constants.REASON_MESSAGE_HEADER, String.class));
                            capiRestError.setErrorCode(exchange.getIn().getHeader(Constants.REASON_CODE_HEADER, Integer.class));
                            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(Constants.REASON_CODE_HEADER));
                        } else {
                            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400));
                            capiRestError.setErrorMessage("Unknown error");
                            capiRestError.setErrorCode(500);
                        }
                        exchange.getIn().setBody(httpUtils.proxyErrorMapper(capiRestError));
                    }
                })
                .removeHeader(Constants.REASON_MESSAGE_HEADER)
                .removeHeader(Constants.REASON_CODE_HEADER)
                .routeId("error-route");
    }
}