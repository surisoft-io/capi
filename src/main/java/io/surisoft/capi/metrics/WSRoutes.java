package io.surisoft.capi.metrics;

import io.surisoft.capi.schema.WebsocketClient;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Endpoint(id = "ws-routes")
public class WSRoutes {

    private final Optional<Map<String, WebsocketClient>> websocketClientMap;

    public WSRoutes(Optional<Map<String, WebsocketClient>> websocketClientMap) {
        this.websocketClientMap = websocketClientMap;
    }

    @ReadOperation
    public Map<String, WebsocketClient> getAllWebsocketRoutesInfo() {
        return websocketClientMap.orElse(null);
    }
}