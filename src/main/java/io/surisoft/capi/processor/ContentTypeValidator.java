package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ContentTypeValidator implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ContentTypeValidator.class);
    @Override
    public void process(Exchange exchange) throws Exception {
        String contentType = (String) exchange.getIn().getHeader(Constants.CONTENT_TYPE);
        String accptType = (String) exchange.getIn().getHeader(Constants.ACCEPT_TYPE);
        if(contentType == null) {
            contentType = (String) exchange.getIn().getHeader("content-type");
        }
        if(accptType == null) {
            accptType = (String) exchange.getIn().getHeader("accept");
        }

        if (contentType != null && contentType.equalsIgnoreCase("text/event-stream")) {
            log.warn("Content type is not supported");
            sendException(exchange);
        }
        if (accptType != null && accptType.equalsIgnoreCase("text/event-stream")) {
            log.warn("Content type is not supported");
            sendException(exchange);
        }
    }

    private void sendException(Exchange exchange) {
        exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, 400);
        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "Event Stream not allowed in this route, contact your System Administrator");
        exchange.setException(new AuthorizationException("Event Stream not allowed in this route, contact your System Administrator"));
    }
}