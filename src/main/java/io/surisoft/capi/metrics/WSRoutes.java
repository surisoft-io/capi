package io.surisoft.capi.metrics;

import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.WebsocketClient;
import io.surisoft.capi.utils.ServiceUtils;
import org.cache2k.Cache;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Endpoint(id = "ws-routes")
public class WSRoutes {

    private final ServiceUtils serviceUtils;
    private final Cache<String, Service> serviceCache;
    private final Optional<Map<String, WebsocketClient>> websocketClientMap;

    public WSRoutes(ServiceUtils serviceUtils,
                    Cache<String, Service> serviceCache,
                    Optional<Map<String, WebsocketClient>> websocketClientMap) {
        this.serviceUtils = serviceUtils;
        this.serviceCache = serviceCache;
        this.websocketClientMap = websocketClientMap;
    }

    /*@ReadOperation
    public Service getCachedService(@Selector String serviceName) {
        if(serviceCache.containsKey(serviceName)) {
            return serviceCache.get(serviceName);
        }
        return null;
    }*/

    @ReadOperation
    public Map<String, WebsocketClient> getAllWebsocketRoutesInfo() {
        return websocketClientMap.orElse(null);
    }
}