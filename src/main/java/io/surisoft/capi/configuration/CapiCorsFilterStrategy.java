package io.surisoft.capi.configuration;

import io.surisoft.capi.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapiCorsFilterStrategy implements HeaderFilterStrategy {

    private static final Logger log = LoggerFactory.getLogger(CapiCorsFilterStrategy.class);

    public CapiCorsFilterStrategy() {
        log.info("Capi Filter Strategy initialized");
    }

    @Override
    public boolean applyFilterToCamelHeaders(String headerName, Object headerValue, Exchange exchange) {
        return false;
    }

    @Override
    public boolean applyFilterToExternalHeaders(String headerName, Object headerValue, Exchange exchange) {
        if(headerName.equals(Constants.ACCESS_CONTROL_ALLOW_ORIGIN)) {
            return true;
        }
        return Constants.CAPI_CORS_MANAGED_HEADERS.containsKey(headerName);
    }
}