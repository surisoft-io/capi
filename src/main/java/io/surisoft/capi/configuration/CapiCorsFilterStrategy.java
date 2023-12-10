package io.surisoft.capi.configuration;

import io.surisoft.capi.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapiCorsFilterStrategy extends DefaultHeaderFilterStrategy {

    private static final Logger log = LoggerFactory.getLogger(CapiCorsFilterStrategy.class);

    public CapiCorsFilterStrategy() {
        log.info("Capi Filter Strategy initialized");
        initialize();
    }

    protected void initialize() {
        getOutFilter().add("content-length");
        getOutFilter().add("content-type");
        getOutFilter().add("host");
        getOutFilter().add("cache-control");
        getOutFilter().add("connection");
        getOutFilter().add("date");
        getOutFilter().add("pragma");
        getOutFilter().add("trailer");
        getOutFilter().add("transfer-encoding");
        getOutFilter().add("upgrade");
        getOutFilter().add("via");
        getOutFilter().add("warning");
        getOutFilter().add(Constants.ACCESS_CONTROL_ALLOW_ORIGIN);

        Constants.CAPI_CORS_MANAGED_HEADERS.forEach((key, value) -> {
            getOutFilter().add(key);
        });

        setLowerCase(true);

        // filter headers begin with "Camel" or "org.apache.camel"
        // must ignore case for Http based transports
        setOutFilterStartsWith(CAMEL_FILTER_STARTS_WITH);
        setInFilterStartsWith(CAMEL_FILTER_STARTS_WITH);
    }

    @Override
    public boolean applyFilterToExternalHeaders(String headerName, Object headerValue, Exchange exchange) {
        if(headerName.equalsIgnoreCase(Constants.ACCESS_CONTROL_ALLOW_ORIGIN)) {
            return true;
        }
        if(Constants.CAPI_CORS_MANAGED_HEADERS.containsKey(headerName)) {
            return true;
        }
        return super.applyFilterToExternalHeaders(headerName, headerValue, exchange);
    }
}