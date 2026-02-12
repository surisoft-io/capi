package io.surisoft.capi.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class KafkaProcessorTest {

    private KafkaProcessor processorUnderTest;

    private KafkaProcessor createProcessor(boolean sslEnabled) {
        KafkaProcessor processor = new KafkaProcessor();
        ReflectionTestUtils.setField(processor, "capiKafkaHost", "kafka-broker:9092");
        ReflectionTestUtils.setField(processor, "capiKafkaTopic", "capi-events");
        ReflectionTestUtils.setField(processor, "capiKafkaGroupInstance", "instance-1");
        ReflectionTestUtils.setField(processor, "capiKafkaGroupId", "capi-group");
        ReflectionTestUtils.setField(processor, "capiKafkaSslEnabled", sslEnabled);
        ReflectionTestUtils.setField(processor, "capiKafkaSslKeystoreLocation", "/ssl/keystore.jks");
        ReflectionTestUtils.setField(processor, "capiKafkaSslKeystorePassword", "ks-pass");
        ReflectionTestUtils.setField(processor, "capiKafkaSslTruststoreLocation", "/ssl/truststore.jks");
        ReflectionTestUtils.setField(processor, "capiKafkaSslTruststorePassword", "ts-pass");
        return processor;
    }

    @Test
    void sslDisabled_endpointHasBasicKafkaParamsNoSsl() {
        processorUnderTest = createProcessor(false);

        String endpoint = processorUnderTest.buildEndpoint();

        assertThat(endpoint).startsWith("capi-events?");
        assertThat(endpoint).contains("brokers=kafka-broker:9092");
        assertThat(endpoint).contains("groupId=capi-group");
        assertThat(endpoint).contains("autoOffsetReset=latest");
        assertThat(endpoint).contains("consumersCount=1");
        assertThat(endpoint).contains("valueDeserializer=io.surisoft.capi.kafka.CapiKafkaEventDeserializer");
        assertThat(endpoint).doesNotContain("securityProtocol=SSL");
        assertThat(endpoint).doesNotContain("sslKeystoreLocation");
        assertThat(endpoint).doesNotContain("sslTruststoreLocation");
    }

    @Test
    void sslEnabled_endpointHasSslKeystoreTruststoreParams() {
        processorUnderTest = createProcessor(true);

        String endpoint = processorUnderTest.buildEndpoint();

        assertThat(endpoint).startsWith("capi-events?");
        assertThat(endpoint).contains("brokers=kafka-broker:9092");
        assertThat(endpoint).contains("securityProtocol=SSL");
        assertThat(endpoint).contains("sslKeystoreLocation=/ssl/keystore.jks");
        assertThat(endpoint).contains("sslKeystorePassword=ks-pass");
        assertThat(endpoint).contains("sslKeyPassword=ks-pass");
        assertThat(endpoint).contains("sslTruststoreLocation=/ssl/truststore.jks");
        assertThat(endpoint).contains("sslTruststorePassword=ts-pass");
        assertThat(endpoint).contains("groupInstanceId=instance-1");
        assertThat(endpoint).contains("groupId=capi-group");
        assertThat(endpoint).contains("valueDeserializer=io.surisoft.capi.kafka.CapiKafkaEventDeserializer");
    }
}
