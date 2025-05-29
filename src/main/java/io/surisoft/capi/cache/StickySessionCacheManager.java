package io.surisoft.capi.cache;

import io.surisoft.capi.kafka.CapiInstance;
import io.surisoft.capi.kafka.CapiKafkaEvent;
import io.surisoft.capi.schema.CapiEvent;
import io.surisoft.capi.schema.StickySession;
import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(value = "capi.kafka.enabled", havingValue = "true")
public class StickySessionCacheManager {
    private final Cache<String, StickySession> stickySessionCache;
    private final KafkaTemplate<String, CapiEvent> kafkaTemplate;
    private final CapiInstance capiInstance;
    private final String capiKafkaTopic;

    public StickySessionCacheManager(Cache<String, StickySession> stickySessionCache,
                                     KafkaTemplate<String, CapiEvent> kafkaTemplate,
                                     CapiInstance capiInstance,
                                     @Value("${capi.kafka.topic}") String capiKafkaTopic) {
        this.stickySessionCache = stickySessionCache;
        this.kafkaTemplate = kafkaTemplate;
        this.capiInstance = capiInstance;
        this.capiKafkaTopic = capiKafkaTopic;
    }

    public void createStickySession(StickySession stickySession, boolean notifyOtherInstances) {
        notifyOtherInstances(notifyOtherInstances, stickySession);
        stickySession.setId(stickySession.getParamName() + ":" + stickySession.getParamValue());
        stickySessionCache.put(stickySession.getId(), stickySession);
    }

    public StickySession getStickySessionById(String paramName, String paramValue) {
        String stickySessionId = paramName + ":" + paramValue;
        return stickySessionCache.peek(stickySessionId);
    }

    public void deleteStickySession(StickySession stickySession) {
        stickySessionCache.remove(stickySession.getId());
    }

    private void notifyOtherInstances(boolean notifyOtherInstances, StickySession stickySession) {
        if(notifyOtherInstances) {
            CapiEvent capiEvent = new CapiEvent();
            capiEvent.setId(UUID.randomUUID().toString());
            //capiEvent.setInstanceId(capiInstance);
            capiEvent.setKey(stickySession.getParamName());
            capiEvent.setValue(stickySession.getParamValue());
            capiEvent.setNodeIndex(stickySession.getNodeIndex());
            capiEvent.setType(CapiKafkaEvent.STICKY_SESSION_EVENT_TYPE);
            kafkaTemplate.send(capiKafkaTopic, capiEvent);
        }
    }
}