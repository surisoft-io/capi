package io.surisoft.capi.processor;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.RequiredSearch;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetricsProcessor implements Processor {

    @Autowired
    private CompositeMeterRegistry meterRegistry;

    @Override
    public void process(Exchange exchange) {
        if(exchange.getFromRouteId() != null) {
            RequiredSearch requiredSearch = meterRegistry.get(exchange.getFromRouteId());
            requiredSearch.counter().increment();
        }
    }
}