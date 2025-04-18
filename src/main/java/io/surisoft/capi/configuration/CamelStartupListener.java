package io.surisoft.capi.configuration;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedStartupListener;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelStartupListener implements ExtendedStartupListener {

    private static final Logger log = LoggerFactory.getLogger(CamelStartupListener.class);
    private final long consulTimerInterval;
    private final boolean capiConsulEnabled;

    public CamelStartupListener(long consulTimerInterval, boolean capiConsulEnabled) {
        this.consulTimerInterval = consulTimerInterval;
        this.capiConsulEnabled = capiConsulEnabled;
    }

    @Override
    public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
    }

    @Override
    public void onCamelContextFullyStarted(CamelContext context, boolean alreadyStarted) throws Exception {
        if(capiConsulEnabled) {
            context.addRoutes(routeBuilder());
        }
    }

    public RouteBuilder routeBuilder() {
        log.debug("Creating Capi Consul Node Discovery");
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:consul-inspect?period=" + consulTimerInterval)
                        .to("bean:consulNodeDiscovery?method=processInfo")
                        .routeId("consul-discovery-service");
            }
        };
    }
}