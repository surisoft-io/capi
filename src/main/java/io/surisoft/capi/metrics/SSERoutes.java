package io.surisoft.capi.metrics;

import io.surisoft.capi.schema.SSEClient;
import io.surisoft.capi.schema.WebsocketClient;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Endpoint(id = "sseroutes")
public class SSERoutes {

    private final Optional<Map<String, SSEClient>> sseClientMap;

    public SSERoutes(Optional<Map<String, SSEClient>> sseClientMap) {
        this.sseClientMap = sseClientMap;
    }

    @ReadOperation
    public Map<String, SSEClient> getAllSSERoutesInfo() {
        return sseClientMap.orElse(null);
    }
}