package io.surisoft.capi.builder;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ConsistencyRouteBuilder extends RouteBuilder {
    @Override
    public void configure() {
        log.debug("Creating CAPI Consistency Checker");
        from("timer:consistency-checker?period=20000")
                .to("bean:consistencyChecker?method=process")
                .routeId("consistency-checker-service");
    }
}