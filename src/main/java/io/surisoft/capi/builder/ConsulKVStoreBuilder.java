package io.surisoft.capi.builder;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "capi.consul.kv", name = "enabled", havingValue = "true")
public class ConsulKVStoreBuilder extends RouteBuilder {

    private final int interval;

    public ConsulKVStoreBuilder(@Value("${capi.consul.kv.timer.interval}") int interval) {
        this.interval = interval;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Creating CAPI Consul KV Store");
        from("timer:consul-KV-Store?period=" + interval)
                .to("bean:consulKVStore?method=process")
                .routeId("consul-key-value-store");
    }
}