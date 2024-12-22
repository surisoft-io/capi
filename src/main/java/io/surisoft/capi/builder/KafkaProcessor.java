package io.surisoft.capi.builder;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "capi.kafka", name = "enabled", havingValue = "true")
public class KafkaProcessor extends RouteBuilder {

    @Value("${capi.kafka.host}")
    private String capiKafkaHost;

    @Value("${capi.kafka.topic}")
    private String capiKafkaTopic;

    @Value("${capi.kafka.group-instance}")
    private String capiKafkaGroupInstance;

    @Value("${capi.kafka.group-id}")
    private String capiKafkaGroupId;

    @Value("${capi.kafka.ssl.enabled}")
    private boolean capiKafkaSslEnabled;

    @Value("${capi.kafka.ssl.keystore.location}")
    private String capiKafkaSslKeystoreLocation;

    @Value("${capi.kafka.ssl.keystore.password}")
    private String capiKafkaSslKeystorePassword;

    @Value("${capi.kafka.ssl.truststore.location}")
    private String capiKafkaSslTruststoreLocation;

    @Value("${capi.kafka.ssl.truststore.password}")
    private String capiKafkaSslTruststorePassword;

    @Override
    public void configure() {
        from("kafka:" + buildEndpoint()).to("bean:capiKafkaEventProcessor?method=process(${body})");
    }

    private String buildEndpoint() {
        if(capiKafkaSslEnabled) {
           return capiKafkaTopic +
                    "?brokers=" + capiKafkaHost +
                    "&securityProtocol=SSL" +
                    "&sslKeystoreLocation=" + capiKafkaSslKeystoreLocation +
                    "&sslKeystorePassword=" + capiKafkaSslKeystorePassword +
                    "&sslKeyPassword=" +  capiKafkaSslKeystorePassword +
                    "&sslTruststoreLocation=" + capiKafkaSslTruststoreLocation +
                    "&sslTruststorePassword=" + capiKafkaSslTruststorePassword +
                    "&groupInstanceId=" + capiKafkaGroupInstance +
                    "&autoOffsetReset=latest" +
                    "&groupId=" + capiKafkaGroupId +
                    "&valueDeserializer=io.surisoft.capi.kafka.CapiKafkaEventDeserializer";
        } else {
            return capiKafkaTopic +
                    "?brokers=" + capiKafkaHost +
                    "&groupId=" + capiKafkaGroupId +
                    "&autoOffsetReset=latest" +
                    "&consumersCount=1" +
                    "&valueDeserializer=io.surisoft.capi.kafka.CapiKafkaEventDeserializer";
        }
    }
}