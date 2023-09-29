package io.surisoft.capi.kafka;

import io.surisoft.capi.cache.StickySessionCacheManager;
import io.surisoft.capi.schema.CapiEvent;
import io.surisoft.capi.schema.StickySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "capi.kafka", name = "enabled", havingValue = "true")
public class CapiKafkaEvent {

    public static final String STICKY_SESSION_EVENT_TYPE = "sticky-session";
    public static final String CERTIFICATE_CHANGE_EVENT_TYPE = "certificate-change";
    @Autowired
    private CapiInstance capiInstance;
    @Autowired
    private StickySessionCacheManager stickySessionCacheManager;


    private static final Logger log = LoggerFactory.getLogger(CapiKafkaEvent.class);
    public void process(CapiEvent incomingEvent) {
        if(incomingEvent.getInstanceId().equals(capiInstance)) {
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
        log.trace("Received event: {}", incomingEvent);

    }

    @Bean(name = "capiKafkaEventProcessor")
    public CapiKafkaEvent capiKafkaEvent() {
        return this;
    }
}
