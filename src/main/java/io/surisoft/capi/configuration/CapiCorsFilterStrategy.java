package io.surisoft.capi.configuration;

import io.surisoft.capi.utils.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CapiCorsFilterStrategy extends DefaultHeaderFilterStrategy {

    private static final Logger log = LoggerFactory.getLogger(CapiCorsFilterStrategy.class);
    private final List<String> allowedHeaders;
    private Map<String, String> managedHeaders;

    public CapiCorsFilterStrategy(List<String> allowedHeaders) {
        log.info("Capi Filter Strategy initialized");
        this.allowedHeaders = allowedHeaders;
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

        managedHeaders = new java.util.HashMap<>(Constants.CAPI_CORS_MANAGED_HEADERS);
        managedHeaders.put("Access-Control-Allow-Headers", StringUtils.join(allowedHeaders, ","));
        managedHeaders.forEach((key, value) -> {
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
        if(managedHeaders.containsKey(headerName)) {
            return true;
        }
        return super.applyFilterToExternalHeaders(headerName, headerValue, exchange);
    }
}