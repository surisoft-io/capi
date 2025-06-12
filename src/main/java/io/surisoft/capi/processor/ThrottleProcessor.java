package io.surisoft.capi.processor;

import io.surisoft.capi.exception.AuthorizationException;
import io.surisoft.capi.kafka.CapiInstance;
import io.surisoft.capi.kafka.CapiKafkaEvent;
import io.surisoft.capi.oidc.Oauth2Constants;
import io.surisoft.capi.schema.CapiEvent;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ThrottleServiceObject;
import io.surisoft.capi.utils.Constants;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.cache2k.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "capi.throttling", name = "enabled", havingValue = "true")
public class ThrottleProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(ThrottleProcessor.class);
    private final Cache<String, Service> serviceCache;
    private final HttpUtils httpUtils;

    private final Cache<String, ThrottleServiceObject> throttleServiceObjectCache;
    private final KafkaTemplate<String, CapiEvent> kafkaTemplate;
    private final String capiKafkaTopic;
    private final CapiInstance capiInstance;

    public ThrottleProcessor(Cache<String, Service> serviceCache,
                             HttpUtils httpUtils, Cache<String,
                             ThrottleServiceObject> throttleServiceObjectCache,
                             KafkaTemplate<String, CapiEvent> kafkaTemplate,
                             @Value("${capi.kafka.topic}") String capiKafkaTopic,
                             CapiInstance capiInstance) {
        this.serviceCache = serviceCache;
        this.httpUtils = httpUtils;
        this.throttleServiceObjectCache = throttleServiceObjectCache;
        this.kafkaTemplate = kafkaTemplate;
        this.capiKafkaTopic = capiKafkaTopic;
        this.capiInstance = capiInstance;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            String contextPath = (String) exchange.getIn().getHeader(Oauth2Constants.CAMEL_SERVLET_CONTEXT_PATH);
            Service service = serviceCache.get(httpUtils.contextToRole(contextPath));
            if(service != null) {
                if(service.getServiceMeta().isThrottleGlobal() &&
                        service.getServiceMeta().getThrottleDuration() > -1 &&
                        service.getServiceMeta().getThrottleTotalCalls() > -1) {
                    if(!canContinue(exchange, service, null, false, -1, -1)) {
                        exchange.setProperty(Constants.REASON_MESSAGE_HEADER, "Too Many requests");

                        exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "Too Many requests");
                        exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, HttpStatus.TOO_MANY_REQUESTS.value());
                        exchange.setException(new AuthorizationException("Too Many requests"));
                    }
                } else if(!service.getServiceMeta().isThrottleGlobal()) {
                    //Here we should expect token claims, that we should remove later, before returning to the client.
                    if(exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_CONSUMER_KEY) != null &&
                            exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_DURATION) != null &&
                            exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_TOTAL_CALLS_ALLOWED) != null) {
                        if(!canContinue(exchange, service, (String) exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_CONSUMER_KEY), true, (Long) exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_TOTAL_CALLS_ALLOWED), (Long) exchange.getIn().getHeader(Constants.CAPI_META_THROTTLE_DURATION))) {
                            exchange.setProperty(Constants.REASON_MESSAGE_HEADER, "Too Many requests");

                            exchange.getIn().setHeader(Constants.REASON_MESSAGE_HEADER, "Too Many requests");
                            exchange.getIn().setHeader(Constants.REASON_CODE_HEADER, HttpStatus.TOO_MANY_REQUESTS.value());
                            exchange.setException(new AuthorizationException("Too Many requests"));
                        }
                    }
                }
            }
        } catch(Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    public boolean canContinue(Exchange exchange, Service service, String consumerKey, boolean consumerThrottle, long totalCallsAllowed, long expirationDuration) {
        String cacheKey = consumerThrottle ? service.getId() + ":" + consumerKey : service.getId();
        ThrottleServiceObject throttleServiceObject = throttleServiceObjectCache.get(cacheKey);

        if(!consumerThrottle) {
            totalCallsAllowed = service.getServiceMeta().getThrottleTotalCalls();
            expirationDuration = service.getServiceMeta().getThrottleDuration();
        }

        if (throttleServiceObject == null || throttleServiceObject.isExpired()) {
            throttleServiceObject = new ThrottleServiceObject(cacheKey, consumerThrottle ? consumerKey : null, totalCallsAllowed, expirationDuration);
            throttleServiceObjectCache.put(cacheKey, throttleServiceObject);
            sendToKafka(throttleServiceObject);

            exchange.setProperty(Constants.CAPI_META_THROTTLE_CURRENT_CALL_NUMBER, throttleServiceObject.getCurrentCalls());

            return true;
        }

        if (throttleServiceObject.canCall()) {
            throttleServiceObject.increment();
            throttleServiceObjectCache.put(cacheKey, throttleServiceObject);
            sendToKafka(throttleServiceObject);

            exchange.setProperty(Constants.CAPI_META_THROTTLE_CURRENT_CALL_NUMBER, throttleServiceObject.getCurrentCalls());

            return true;
        }

        return false;
    }

    private void sendToKafka(ThrottleServiceObject throttleServiceObject) {
        CapiEvent capiEvent = new CapiEvent();
        capiEvent.setId(UUID.randomUUID().toString());
        capiEvent.setInstanceId(capiInstance.uuid());

        capiEvent.setKey(throttleServiceObject.getCacheKey());
        capiEvent.setThrottleServiceObject(throttleServiceObject);
        capiEvent.setType(CapiKafkaEvent.THROTTLING_EVENT_TYPE);
        log.trace("Sending Kafka CAPI event: {}", capiEvent.getId());
        kafkaTemplate.send(capiKafkaTopic, capiEvent);
    }
}