package io.surisoft.capi.processor;

import io.surisoft.capi.kafka.CapiInstance;
import io.surisoft.capi.schema.CapiEvent;
import io.surisoft.capi.schema.Service;
import io.surisoft.capi.schema.ServiceMeta;
import io.surisoft.capi.schema.ThrottleServiceObject;
import io.surisoft.capi.utils.HttpUtils;
import org.apache.camel.Exchange;
import org.cache2k.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThrottleProcessorTest {

    @Mock
    private Cache<String, Service> serviceCache;
    @Mock
    private HttpUtils httpUtils;
    @Mock
    private Cache<String, ThrottleServiceObject> throttleServiceObjectCache;
    @Mock
    private KafkaTemplate<String, CapiEvent> kafkaTemplate;
    @Mock
    private Exchange exchange;

    private ThrottleProcessor processorUnderTest;

    @BeforeEach
    void setUp() {
        CapiInstance capiInstance = new CapiInstance("test-instance-id");
        processorUnderTest = new ThrottleProcessor(
                serviceCache, httpUtils, throttleServiceObjectCache,
                kafkaTemplate, "capi-events", capiInstance
        );
    }

    @Test
    void cacheMiss_createsNewEntryAndReturnsTrue() {
        Service service = createService("svc-1");
        when(throttleServiceObjectCache.get("svc-1")).thenReturn(null);

        boolean result = processorUnderTest.canContinue(exchange, service, null, false, -1, -1);

        assertThat(result).isTrue();
        verify(throttleServiceObjectCache).put(eq("svc-1"), any(ThrottleServiceObject.class));
        verify(kafkaTemplate).send(eq("capi-events"), any(CapiEvent.class));
    }

    @Test
    void expiredEntry_createsNewEntryAndReturnsTrue() {
        Service service = createService("svc-1");
        ThrottleServiceObject expired = new ThrottleServiceObject("svc-1", null, 100, -1000);
        when(throttleServiceObjectCache.get("svc-1")).thenReturn(expired);

        boolean result = processorUnderTest.canContinue(exchange, service, null, false, -1, -1);

        assertThat(result).isTrue();
        verify(throttleServiceObjectCache).put(eq("svc-1"), any(ThrottleServiceObject.class));
    }

    @Test
    void withinLimit_incrementsAndReturnsTrue() {
        Service service = createService("svc-1");
        ThrottleServiceObject active = new ThrottleServiceObject("svc-1", null, 100, 60000);
        when(throttleServiceObjectCache.get("svc-1")).thenReturn(active);

        boolean result = processorUnderTest.canContinue(exchange, service, null, false, -1, -1);

        assertThat(result).isTrue();
        verify(throttleServiceObjectCache).put(eq("svc-1"), eq(active));
    }

    @Test
    void limitExceeded_returnsFalse() {
        Service service = createService("svc-1");
        ThrottleServiceObject atLimit = new ThrottleServiceObject("svc-1", null, 1, 60000);
        when(throttleServiceObjectCache.get("svc-1")).thenReturn(atLimit);

        boolean result = processorUnderTest.canContinue(exchange, service, null, false, -1, -1);

        assertThat(result).isFalse();
    }

    @Test
    void globalKey_usesServiceId() {
        Service service = createService("my-service");
        when(throttleServiceObjectCache.get("my-service")).thenReturn(null);

        processorUnderTest.canContinue(exchange, service, null, false, -1, -1);

        verify(throttleServiceObjectCache).get("my-service");
        verify(throttleServiceObjectCache).put(eq("my-service"), any(ThrottleServiceObject.class));
    }

    @Test
    void consumerKey_usesServiceIdColonConsumerKey() {
        Service service = createService("my-service");
        when(throttleServiceObjectCache.get("my-service:user-123")).thenReturn(null);

        processorUnderTest.canContinue(exchange, service, "user-123", true, 100, 60000);

        verify(throttleServiceObjectCache).get("my-service:user-123");
        verify(throttleServiceObjectCache).put(eq("my-service:user-123"), any(ThrottleServiceObject.class));
    }

    @Test
    void sendsKafkaEventOnEachSuccessfulContinue() {
        Service service = createService("svc-1");
        when(throttleServiceObjectCache.get("svc-1")).thenReturn(null);

        processorUnderTest.canContinue(exchange, service, null, false, -1, -1);

        ArgumentCaptor<CapiEvent> eventCaptor = ArgumentCaptor.forClass(CapiEvent.class);
        verify(kafkaTemplate).send(eq("capi-events"), eventCaptor.capture());
        CapiEvent event = eventCaptor.getValue();
        assertThat(event.getInstanceId()).isEqualTo("test-instance-id");
        assertThat(event.getKey()).isEqualTo("svc-1");
        assertThat(event.getType()).isEqualTo("throttling");
    }

    private Service createService(String id) {
        Service service = new Service();
        service.setId(id);
        ServiceMeta meta = new ServiceMeta();
        meta.setThrottleTotalCalls(100);
        meta.setThrottleDuration(60000);
        service.setServiceMeta(meta);
        return service;
    }
}
