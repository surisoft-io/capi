package io.surisoft.capi.kafka;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.schema.CapiEvent;
import io.surisoft.capi.schema.StickySession;
import io.surisoft.capi.schema.ThrottleServiceObject;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "capi.kafka", name = "enabled", havingValue = "true")
public class CapiKafkaEvent {

    public static final String STICKY_SESSION_EVENT_TYPE = "sticky-session";
    public static final String THROTTLING_EVENT_TYPE = "throttling";

    private final CapiInstance capiInstance;
    private final StickySessionCacheManager stickySessionCacheManager;
    private final Cache<String, ThrottleServiceObject> throttleServiceObjectCache;

    public CapiKafkaEvent(CapiInstance capiInstance,
                          StickySessionCacheManager stickySessionCacheManager,
                          Cache<String, ThrottleServiceObject> throttleServiceObjectCache) {
        this.capiInstance = capiInstance;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.throttleServiceObjectCache = throttleServiceObjectCache;

        log.trace("CAPI Instance ID: {}", capiInstance.uuid());

    }

    private static final Logger log = LoggerFactory.getLogger(CapiKafkaEvent.class);
    public void process(CapiEvent incomingEvent) {
        //log.trace(capiInstance.uuid());
        if(incomingEvent.getInstanceId().equals(capiInstance.uuid())) {
            log.trace("Event {} is from this instance, ignoring", incomingEvent.getId());
            return;
        }
        if(incomingEvent.getType().equals(STICKY_SESSION_EVENT_TYPE)) {
            StickySession stickySession = new StickySession();
            stickySession.setParamValue(incomingEvent.getValue());
            stickySession.setParamName(incomingEvent.getKey());
            stickySession.setNodeIndex(incomingEvent.getNodeIndex());
            stickySessionCacheManager.createStickySession(stickySession, false);
            log.trace("Event {} is a sticky session event", incomingEvent.getId());
            return;
        }
        if(incomingEvent.getType().equals(THROTTLING_EVENT_TYPE) && incomingEvent.getThrottleServiceObject() != null) {
            log.trace("Event {} is a throttling service object", incomingEvent.getKey());

            log.trace("Incoming Object");
            log.trace(incomingEvent.getThrottleServiceObject().getCacheKey());
            log.trace("{}", incomingEvent.getThrottleServiceObject().getTotalCallsAllowed());
            log.trace("{}", incomingEvent.getThrottleServiceObject().getCurrentCalls());
            log.trace("{}", incomingEvent.getThrottleServiceObject().getExpirationTime());

            throttleServiceObjectCache.put(incomingEvent.getKey(), incomingEvent.getThrottleServiceObject());
            return;
        }
        log.trace("Received event: {}", incomingEvent);
    }

    @Bean(name = "capiKafkaEventProcessor")
    public CapiKafkaEvent capiKafkaEvent() {
        return this;
    }
}
