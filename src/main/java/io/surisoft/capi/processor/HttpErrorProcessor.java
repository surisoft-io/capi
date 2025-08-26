package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Component
public class HttpErrorProcessor implements Processor {
    @Override
    public void process(Exchange exchange) {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if(cause instanceof SSLHandshakeException) {
            exchange.setProperty(Constants.REASON_MESSAGE_HEADER, "Problem with Service certificate");
            exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "Problem with Service certificate");
            exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, 502);
        }
        if (cause instanceof SSLException) {
            exchange.setProperty(Constants.REASON_MESSAGE_HEADER, "Problem with Service certificate");
            exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "Problem with Service certificate");
            exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, 502);
        } else if (cause instanceof UnknownHostException) {
            exchange.setProperty(Constants.REASON_MESSAGE_HEADER, "Problem with Service host");
            exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "Problem with Service host");
            exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, 502);
        } else if (cause instanceof SocketTimeoutException) {
            exchange.setProperty(Constants.REASON_MESSAGE_HEADER, "The remote server took too long");
            exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "The remote server took too long");
            exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, 502);
        } else if(cause instanceof HttpHostConnectException) {
            exchange.setProperty(Constants.REASON_MESSAGE_HEADER, "No server available at the moment. Please try again later.");
            exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "No server available at the moment. Please try again later.");
            exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, 502);
        } else if(cause instanceof AuthorizationException) {
            exchange.setProperty(Constants.REASON_MESSAGE_HEADER, cause.getMessage());
            exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, cause.getMessage());
            exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, 400);
        }
        exchange.getIn().setHeader(Constants.CAPI_URI_IN_ERROR, exchange.getIn().getHeader(Exchange.HTTP_URI).toString());
        exchange.getIn().setHeader(Constants.CAPI_URL_IN_ERROR, exchange.getIn().getHeader(Exchange.HTTP_URL).toString());
    }
}