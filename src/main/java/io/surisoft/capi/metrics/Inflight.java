package io.surisoft.capi.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.InflightRepository;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "inflight")
public class Inflight {

    private final MeterRegistry meterRegistry;
    private final CamelContext camelContext;

    public Inflight(MeterRegistry meterRegistry,
                    CamelContext camelContext) {
        this.camelContext = camelContext;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void post() {
        meterRegistry.gauge("camel_inflight_total", camelContext.getInflightRepository(),
                InflightRepository::size);
    }

    @ReadOperation
    public int getInflightTotal() {
        return camelContext.getInflightRepository().size();
    }
}